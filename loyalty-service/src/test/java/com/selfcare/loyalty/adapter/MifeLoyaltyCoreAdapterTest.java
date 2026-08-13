package com.selfcare.loyalty.adapter;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.selfcare.loyalty.adapter.LoyaltyCoreAdapter.DonateCommand;
import com.selfcare.loyalty.adapter.LoyaltyCoreAdapter.PointsBalance;
import com.selfcare.loyalty.adapter.LoyaltyCoreAdapter.PointsHistory;
import com.selfcare.loyalty.adapter.LoyaltyCoreAdapter.RegisterCommand;
import com.selfcare.loyalty.adapter.LoyaltyCoreAdapter.RegistrationResult;
import com.selfcare.loyalty.adapter.LoyaltyCoreAdapter.RegistrationStatus;
import com.selfcare.loyalty.adapter.LoyaltyCoreAdapter.TransferCommand;
import com.selfcare.loyalty.config.MifeProperties;
import com.selfcare.loyalty.config.MifeRestClientConfig;
import com.selfcare.loyalty.exception.LoyaltyCoreIntegrationException;
import com.selfcare.platform.common.adapter.ApiAdapter;
import java.math.BigDecimal;
import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises the adapter against a stubbed MIFE rather than a mocked RestClient, so the request
 * bodies, the URL per operation and the status/statusCode conventions documented on
 * {@link MifeLoyaltyCoreAdapter} are all verified for real. The stubs mirror
 * deploy/local/wiremock/mappings, which back the same endpoints in local dev.
 */
class MifeLoyaltyCoreAdapterTest {

    private static final String BALANCE_PATH = "/apicall/starPointsAPI/balanceCheck/1.0";
    private static final String REGISTER_PATH = "/apicall/starPointsAPI/profileRegisterRequest/1.0";
    private static final String TRANSFER_PATH = "/apicall/starPointsAPI/transferStarPoints/1.0";
    private static final String DONATE_PATH = "/apicall/starPointsAPI/starpointDonate/1.0";
    private static final String HISTORY_PATH = "/apicall/StarpointLoyalitySystem/listRequest/1.0";

    private static final String MSISDN = "94771234567";

    private static WireMockServer mife;

    private MifeLoyaltyCoreAdapter adapter;

    @BeforeAll
    static void startMife() {
        mife = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        mife.start();
    }

    @AfterAll
    static void stopMife() {
        mife.stop();
    }

    @BeforeEach
    void setUp() {
        mife.resetAll();

        MifeProperties properties = new MifeProperties();
        properties.setBaseUrl(mife.baseUrl());
        properties.getBalanceCounter().setAlias("balance-alias");
        properties.getBalanceCounter().setAuth("balance-auth");
        properties.getTransferCounter().setAlias("transfer-alias");
        properties.getTransferCounter().setAuth("transfer-auth");
        properties.getDonateCounter().setAlias("donate-alias");
        properties.getDonateCounter().setAuth("donate-auth");

        adapter = new MifeLoyaltyCoreAdapter(new MifeRestClientConfig().mifeRestClient(properties), properties);
    }

    @Test
    void registersAsTheAllTenantsDefaultAdapter() {
        assertThat(adapter.tenantId()).isEqualTo(ApiAdapter.ALL_TENANTS);
    }

    @Test
    void getBalanceReturnsBalancesOnStatusZero() {
        mife.stubFor(post(urlPathEqualTo(BALANCE_PATH))
                .withRequestBody(matchingJsonPath("$.counterAlias", equalTo("balance-alias")))
                .withRequestBody(matchingJsonPath("$.subscriberValue", equalTo(MSISDN)))
                .willReturn(okJson("{\"status\":0,\"currentBalance\":12500.50,\"redeemableBalance\":10000.25}")));

        PointsBalance balance = adapter.getBalance(MSISDN);

        assertThat(balance.currentBalance()).isEqualByComparingTo("12500.50");
        assertThat(balance.redeemableBalance()).isEqualByComparingTo("10000.25");
    }

    @Test
    void getBalanceTreatsStatus121AsZeroBalanceRatherThanFailure() {
        mife.stubFor(post(urlPathEqualTo(BALANCE_PATH))
                .willReturn(okJson("{\"status\":121,\"errorDesc\":\"not registered\"}")));

        PointsBalance balance = adapter.getBalance(MSISDN);

        assertThat(balance.currentBalance()).isEqualByComparingTo("0");
        assertThat(balance.redeemableBalance()).isEqualByComparingTo("0");
    }

    @Test
    void getBalanceFailsOnAnyOtherStatus() {
        mife.stubFor(post(urlPathEqualTo(BALANCE_PATH))
                .willReturn(okJson("{\"status\":99,\"errorDesc\":\"counter suspended\"}")));

        assertThatThrownBy(() -> adapter.getBalance(MSISDN))
                .isInstanceOf(LoyaltyCoreIntegrationException.class)
                .hasMessageContaining("counter suspended");
    }

    @Test
    void getBalanceWrapsTransportFailures() {
        mife.stubFor(post(urlPathEqualTo(BALANCE_PATH)).willReturn(serverError()));

        assertThatThrownBy(() -> adapter.getBalance(MSISDN))
                .isInstanceOf(LoyaltyCoreIntegrationException.class)
                .hasMessageContaining(BALANCE_PATH);
    }

    @Test
    void registerReportsPinSentWhenCoreIssuedAPin() {
        mife.stubFor(put(urlPathEqualTo(REGISTER_PATH))
                .withRequestBody(matchingJsonPath("$.identificationValue", equalTo("912345678V")))
                .willReturn(okJson("{\"status\":0,\"pinAvailable\":true,\"transactionNumber\":\"TX-1\"}")));

        RegistrationResult result = adapter.register(registerCommand());

        assertThat(result.status()).isEqualTo(RegistrationStatus.PIN_SENT);
        assertThat(result.pinTransactionRef()).isEqualTo("TX-1");
    }

    @Test
    void registerReportsRegisteredWhenNoPinWasIssued() {
        mife.stubFor(put(urlPathEqualTo(REGISTER_PATH))
                .willReturn(okJson("{\"status\":0,\"pinAvailable\":false,\"transactionNumber\":\"TX-2\"}")));

        RegistrationResult result = adapter.register(registerCommand());

        assertThat(result.status()).isEqualTo(RegistrationStatus.REGISTERED);
        assertThat(result.pinTransactionRef()).isEqualTo("TX-2");
    }

    @Test
    void registerFailsOnNonZeroStatus() {
        mife.stubFor(put(urlPathEqualTo(REGISTER_PATH))
                .willReturn(okJson("{\"status\":7,\"errorDesc\":\"already registered\"}")));

        assertThatThrownBy(() -> adapter.register(registerCommand()))
                .isInstanceOf(LoyaltyCoreIntegrationException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void registerFailsWhenStatusIsMissing() {
        mife.stubFor(put(urlPathEqualTo(REGISTER_PATH))
                .willReturn(okJson("{\"transactionNumber\":\"TX-9\"}")));

        assertThatThrownBy(() -> adapter.register(registerCommand()))
                .isInstanceOf(LoyaltyCoreIntegrationException.class)
                .hasMessageContaining("Registration failed");
    }

    @Test
    void registerWrapsTransportFailures() {
        mife.stubFor(put(urlPathEqualTo(REGISTER_PATH)).willReturn(serverError()));

        assertThatThrownBy(() -> adapter.register(registerCommand()))
                .isInstanceOf(LoyaltyCoreIntegrationException.class)
                .hasMessageContaining(REGISTER_PATH);
    }

    @Test
    void transferSendsBothSubscribersAndSucceedsOnStatusCodeZero() {
        mife.stubFor(post(urlPathEqualTo(TRANSFER_PATH))
                .withRequestBody(matchingJsonPath("$.counterAlias", equalTo("transfer-alias")))
                .withRequestBody(matchingJsonPath("$.fromSubscriberValue", equalTo(MSISDN)))
                .withRequestBody(matchingJsonPath("$.toSubscriberValue", equalTo("94779999999")))
                .willReturn(okJson("{\"statusCode\":0}")));

        adapter.transferPoints(new TransferCommand(MSISDN, "94779999999", new BigDecimal("25.00")));

        mife.verify(1, postRequestedFor(urlPathEqualTo(TRANSFER_PATH)));
    }

    @Test
    void transferFailsOnNonZeroStatusCode() {
        mife.stubFor(post(urlPathEqualTo(TRANSFER_PATH))
                .willReturn(okJson("{\"statusCode\":12,\"errorDescription\":\"insufficient points\"}")));

        assertThatThrownBy(() ->
                        adapter.transferPoints(new TransferCommand(MSISDN, "94779999999", new BigDecimal("25.00"))))
                .isInstanceOf(LoyaltyCoreIntegrationException.class)
                .hasMessageContaining("insufficient points");
    }

    @Test
    void transferFailsWhenStatusCodeIsMissing() {
        mife.stubFor(post(urlPathEqualTo(TRANSFER_PATH))
                .willReturn(okJson("{\"errorDescription\":\"missing status\"}")));

        assertThatThrownBy(() ->
                        adapter.transferPoints(new TransferCommand(MSISDN, "94779999999", new BigDecimal("25.00"))))
                .isInstanceOf(LoyaltyCoreIntegrationException.class)
                .hasMessageContaining("missing status");
    }

    @Test
    void donateSucceedsOnStatusCodeZero() {
        mife.stubFor(post(urlPathEqualTo(DONATE_PATH))
                .withRequestBody(matchingJsonPath("$.counterAlias", equalTo("donate-alias")))
                .withRequestBody(matchingJsonPath("$.donationAlias", equalTo("charity")))
                .willReturn(okJson("{\"statusCode\":0}")));

        adapter.donatePoints(new DonateCommand(MSISDN, "charity", new BigDecimal("50.00")));

        mife.verify(1, postRequestedFor(urlPathEqualTo(DONATE_PATH)));
    }

    @Test
    void donateFailsOnNonZeroStatusCode() {
        mife.stubFor(post(urlPathEqualTo(DONATE_PATH))
                .willReturn(okJson("{\"statusCode\":5,\"errorDescription\":\"unknown donation alias\"}")));

        assertThatThrownBy(() -> adapter.donatePoints(new DonateCommand(MSISDN, "nope", new BigDecimal("50.00"))))
                .isInstanceOf(LoyaltyCoreIntegrationException.class)
                .hasMessageContaining("unknown donation alias");
    }

    @Test
    void donateFailsWhenStatusCodeIsMissing() {
        mife.stubFor(post(urlPathEqualTo(DONATE_PATH))
                .willReturn(okJson("{\"errorDescription\":\"missing status\"}")));

        assertThatThrownBy(() -> adapter.donatePoints(new DonateCommand(MSISDN, "nope", new BigDecimal("50.00"))))
                .isInstanceOf(LoyaltyCoreIntegrationException.class)
                .hasMessageContaining("missing status");
    }

    @Test
    void historyCombinesTheSeparateBurnAndEarnLookups() {
        mife.stubFor(post(urlPathEqualTo(HISTORY_PATH))
                .withRequestBody(matchingJsonPath("$.transactionType", equalTo("EARN")))
                .willReturn(okJson(
                        """
                        {"status":0,"transactionDetails":[
                          {"transactionSerial":"E-1","merchant":"Supermarket","amount":450.00,"txDate":"2026-08-09"}
                        ]}""")));
        mife.stubFor(post(urlPathEqualTo(HISTORY_PATH))
                .withRequestBody(matchingJsonPath("$.transactionType", equalTo("BURN")))
                .willReturn(okJson(
                        """
                        {"status":0,"transactionDetails":[
                          {"transactionSerial":"B-1","merchant":"Cinema","amount":2000.00,"txDate":"2026-08-07"}
                        ]}""")));

        PointsHistory history = adapter.getHistory(MSISDN, 10);

        assertThat(history.entries()).hasSize(2);
        assertThat(history.entries())
                .extracting(LoyaltyCoreAdapter.PointsHistoryEntry::transactionSerial)
                .containsExactly("B-1", "E-1");
        assertThat(history.entries().get(0).transactionType()).isEqualTo("BURN");
        assertThat(history.entries().get(0).merchant()).isEqualTo("Cinema");
        assertThat(history.entries().get(0).amount()).isEqualByComparingTo("2000.00");
        assertThat(history.entries().get(0).occurredAt()).isEqualTo("2026-08-07");
        assertThat(history.entries().get(1).transactionType()).isEqualTo("EARN");
    }

    @Test
    void historyTreatsANonZeroStatusAsAnEmptyListRatherThanFailingTheWholeLookup() {
        mife.stubFor(post(urlPathEqualTo(HISTORY_PATH)).willReturn(okJson("{\"status\":404}")));

        assertThat(adapter.getHistory(MSISDN, 10).entries()).isEmpty();
    }

    @Test
    void historyTreatsAMissingStatusAsAnEmptyListRatherThanFailingTheWholeLookup() {
        mife.stubFor(post(urlPathEqualTo(HISTORY_PATH)).willReturn(okJson("{\"transactionDetails\":[]}")));

        assertThat(adapter.getHistory(MSISDN, 10).entries()).isEmpty();
    }

    @Test
    void historyToleratesMissingFieldsInTheCoreResponse() {
        mife.stubFor(post(urlPathEqualTo(HISTORY_PATH))
                .withRequestBody(matchingJsonPath("$.transactionType", equalTo("EARN")))
                .willReturn(okJson("{\"status\":0,\"transactionDetails\":[{\"transactionSerial\":\"E-9\"}]}")));
        mife.stubFor(post(urlPathEqualTo(HISTORY_PATH))
                .withRequestBody(matchingJsonPath("$.transactionType", equalTo("BURN")))
                .willReturn(okJson("{\"status\":0}")));

        PointsHistory history = adapter.getHistory(MSISDN, 10);

        assertThat(history.entries()).hasSize(1);
        assertThat(history.entries().get(0).amount()).isEqualByComparingTo("0");
    }

    @Test
    void statusIsAcceptedWhenTheCoreReturnsItAsAString() {
        mife.stubFor(post(urlPathEqualTo(BALANCE_PATH))
                .willReturn(okJson("{\"status\":\"0\",\"currentBalance\":10,\"redeemableBalance\":5}")));

        assertThat(adapter.getBalance(MSISDN).currentBalance()).isEqualByComparingTo("10");
    }

    @Test
    void unparseableStatusIsTreatedAsAFailure() {
        mife.stubFor(post(urlPathEqualTo(BALANCE_PATH))
                .willReturn(okJson("{\"status\":\"OK\",\"errorDesc\":\"bad payload\"}")));

        assertThatThrownBy(() -> adapter.getBalance(MSISDN)).isInstanceOf(LoyaltyCoreIntegrationException.class);
    }

    @Test
    void emptyResponseBodyIsReportedAsAnIntegrationFailure() {
        mife.stubFor(post(urlPathEqualTo(BALANCE_PATH))
                .willReturn(aResponse().withStatus(200)));

        assertThatThrownBy(() -> adapter.getBalance(MSISDN))
                .isInstanceOf(LoyaltyCoreIntegrationException.class)
                .hasMessageContaining("empty body")
                .hasMessageContaining(BALANCE_PATH);
    }

    @Test
    void helperConversionsHandleNullsNumbersAndBigDecimals() throws Exception {
        assertThat(invokeAsInt(null)).isNull();
        assertThat(invokeAsInt(7)).isEqualTo(7);
        assertThat(invokeAsDecimal(new BigDecimal("12.34"))).isEqualByComparingTo("12.34");
    }

    private static Integer invokeAsInt(Object value) throws Exception {
        Method method = MifeLoyaltyCoreAdapter.class.getDeclaredMethod("asInt", Object.class);
        method.setAccessible(true);
        return (Integer) method.invoke(null, value);
    }

    private static BigDecimal invokeAsDecimal(Object value) throws Exception {
        Method method = MifeLoyaltyCoreAdapter.class.getDeclaredMethod("asDecimal", Object.class);
        method.setAccessible(true);
        return (BigDecimal) method.invoke(null, value);
    }

    private static RegisterCommand registerCommand() {
        return new RegisterCommand(MSISDN, "NIC", "912345678V", "Test User", "Colombo", "test@example.com");
    }
}
