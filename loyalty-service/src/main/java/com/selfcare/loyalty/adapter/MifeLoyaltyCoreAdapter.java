package com.selfcare.loyalty.adapter;

import com.selfcare.loyalty.config.MifeProperties;
import com.selfcare.loyalty.exception.LoyaltyCoreIntegrationException;
import com.selfcare.platform.common.adapter.ApiAdapter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Default {@link LoyaltyCoreAdapter} implementation, faithful to the integration the codebase
 * audit found in {@code scapp/StarPointsController.php}: everything is a POST/GET/PUT to
 * MIFE's {@code starPointsAPI} / {@code StarpointLoyalitySystem} endpoints with a per-operation
 * counter alias/auth pair, and a {@code status}/{@code statusCode} field of {@code 0} on the
 * response body means success. Registered as the {@link ApiAdapter#ALL_TENANTS} default -- an
 * operator whose loyalty core is NOT MIFE Star Points gets its own {@code @Component}
 * implementing {@link LoyaltyCoreAdapter} with {@code tenantId()} returning that operator's
 * tenant ID, and the {@link com.selfcare.platform.common.adapter.ApiAdapterRegistry} will
 * prefer it automatically -- no change needed here or in {@code LoyaltyServiceImpl}.
 */
@Component
public class MifeLoyaltyCoreAdapter implements LoyaltyCoreAdapter {

    private static final Logger log = LoggerFactory.getLogger(MifeLoyaltyCoreAdapter.class);
    private static final String COUNTER_ALIAS = "counterAlias";
    private static final String COUNTER_AUTH = "counterAuth";
    private static final String SUBSCRIBER_TYPE = "subscriberType";
    private static final String SUBSCRIBER_VALUE = "subscriberValue";
    private static final String MOBILE = "MOBILE";
    private static final String STATUS = "status";
    private static final String AMOUNT = "amount";

    private final RestClient restClient;
    private final MifeProperties properties;

    public MifeLoyaltyCoreAdapter(RestClient mifeRestClient, MifeProperties properties) {
        this.restClient = mifeRestClient;
        this.properties = properties;
    }

    @Override
    public String tenantId() {
        return ApiAdapter.ALL_TENANTS;
    }

    @Override
    @SuppressWarnings("unchecked")
    public PointsBalance getBalance(String subscriberMsisdn) {
        Map<String, Object> params = Map.of(
                COUNTER_ALIAS, properties.getBalanceCounter().getAlias(),
                COUNTER_AUTH, properties.getBalanceCounter().getAuth(),
                SUBSCRIBER_TYPE, MOBILE,
                SUBSCRIBER_VALUE, subscriberMsisdn,
                "accessMode", "WEB");

        Map<String, Object> result = post("/apicall/starPointsAPI/balanceCheck/1.0", params);
        Integer status = asInt(result.get(STATUS));
        if (status != null && status == 0) {
            return new PointsBalance(asDecimal(result.get("currentBalance")), asDecimal(result.get("redeemableBalance")));
        }
        if (status != null && status == 121) {
            // Legacy semantics: "not yet registered" -- represented as zero balances here,
            // callers should check registration status separately via register().
            return new PointsBalance(BigDecimal.ZERO, BigDecimal.ZERO);
        }
        throw new LoyaltyCoreIntegrationException("Balance check failed: " + result.get("errorDesc"));
    }

    @Override
    public RegistrationResult register(RegisterCommand command) {
        Map<String, Object> params = Map.of(
                COUNTER_ALIAS, properties.getBalanceCounter().getAlias(),
                COUNTER_AUTH, properties.getBalanceCounter().getAuth(),
                "typeBeans", List.of(Map.of(SUBSCRIBER_TYPE, MOBILE, SUBSCRIBER_VALUE, command.subscriberMsisdn())),
                "identificationType", command.idType(),
                "identificationValue", command.idNumber(),
                "accessMode", "WEB");

        Map<String, Object> result = put("/apicall/starPointsAPI/profileRegisterRequest/1.0", params);
        Integer status = asInt(result.get(STATUS));
        if (status == null || status != 0) {
            throw new LoyaltyCoreIntegrationException("Registration failed: " + result.get("errorDesc"));
        }
        boolean pinAvailable = Boolean.TRUE.equals(result.get("pinAvailable"));
        String transactionNumber = String.valueOf(result.get("transactionNumber"));
        if (pinAvailable) {
            return new RegistrationResult(RegistrationStatus.PIN_SENT, transactionNumber);
        }
        return new RegistrationResult(RegistrationStatus.REGISTERED, transactionNumber);
    }

    @Override
    public void transferPoints(TransferCommand command) {
        Map<String, Object> params = Map.of(
                COUNTER_ALIAS, properties.getTransferCounter().getAlias(),
                COUNTER_AUTH, properties.getTransferCounter().getAuth(),
                "fromSubscriberType", MOBILE,
                "fromSubscriberValue", command.fromSubscriberMsisdn(),
                "toSubscriberType", MOBILE,
                "toSubscriberValue", command.toSubscriberMsisdn(),
                AMOUNT, command.amount(),
                "serviceAccessMode", "WEB");

        Map<String, Object> result = post("/apicall/starPointsAPI/transferStarPoints/1.0", params);
        Integer statusCode = asInt(result.get("statusCode"));
        if (statusCode == null || statusCode != 0) {
            throw new LoyaltyCoreIntegrationException("Transfer failed: " + result.get("errorDescription"));
        }
    }

    @Override
    public void donatePoints(DonateCommand command) {
        Map<String, Object> params = Map.of(
                COUNTER_ALIAS, properties.getDonateCounter().getAlias(),
                COUNTER_AUTH, properties.getDonateCounter().getAuth(),
                SUBSCRIBER_TYPE, MOBILE,
                SUBSCRIBER_VALUE, command.subscriberMsisdn(),
                "donationAlias", command.donationAlias(),
                AMOUNT, command.amount());

        Map<String, Object> result = post("/apicall/starPointsAPI/starpointDonate/1.0", params);
        Integer statusCode = asInt(result.get("statusCode"));
        if (statusCode == null || statusCode != 0) {
            throw new LoyaltyCoreIntegrationException("Donation failed: " + result.get("errorDescription"));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public PointsHistory getHistory(String subscriberMsisdn, int listSize) {
        List<PointsHistoryEntry> burn = fetchTransactions(subscriberMsisdn, "BURN", listSize);
        List<PointsHistoryEntry> earn = fetchTransactions(subscriberMsisdn, "EARN", listSize);
        return new PointsHistory(java.util.stream.Stream.concat(burn.stream(), earn.stream()).toList());
    }

    @SuppressWarnings("unchecked")
    private List<PointsHistoryEntry> fetchTransactions(String subscriberMsisdn, String transactionType, int listSize) {
        Map<String, Object> params = Map.of(
                COUNTER_ALIAS, properties.getBalanceCounter().getAlias(),
                COUNTER_AUTH, properties.getBalanceCounter().getAuth(),
                "queryBY", "SUBSCRIBER",
                SUBSCRIBER_TYPE, MOBILE,
                SUBSCRIBER_VALUE, subscriberMsisdn,
                "transactionType", transactionType,
                "listSize", String.valueOf(listSize),
                "searchRange", String.valueOf(listSize),
                "serviceAccessMode", "WEB");

        Map<String, Object> result = post("/apicall/StarpointLoyalitySystem/listRequest/1.0", params);
        Integer status = asInt(result.get(STATUS));
        if (status == null || status != 0) {
            log.warn("MIFE history lookup ({}) returned non-zero status={}, treating as empty", transactionType, status);
            return List.of();
        }
        List<Map<String, Object>> details = (List<Map<String, Object>>) result.getOrDefault("transactionDetails", List.of());
        return details.stream()
                .map(d -> new PointsHistoryEntry(
                        String.valueOf(d.get("transactionSerial")),
                        transactionType,
                        String.valueOf(d.get("merchant")),
                        asDecimal(d.get("amount")),
                        String.valueOf(d.get("txDate"))))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String path, Map<String, Object> body) {
        try {
            Map<String, Object> response = restClient.post().uri(path).body(body).retrieve().body(Map.class);
            return requireResponseBody("POST", path, response);
        } catch (RestClientException ex) {
            throw new LoyaltyCoreIntegrationException("MIFE call failed: POST " + path, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> put(String path, Map<String, Object> body) {
        try {
            Map<String, Object> response = restClient.put().uri(path).body(body).retrieve().body(Map.class);
            return requireResponseBody("PUT", path, response);
        } catch (RestClientException ex) {
            throw new LoyaltyCoreIntegrationException("MIFE call failed: PUT " + path, ex);
        }
    }

    private static Map<String, Object> requireResponseBody(String method, String path, Map<String, Object> response) {
        if (response == null) {
            throw new LoyaltyCoreIntegrationException("MIFE call returned an empty body: " + method + ' ' + path);
        }
        return response;
    }

    private static Integer asInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException _) {
            return null;
        }
    }

    private static BigDecimal asDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(String.valueOf(value));
    }
}
