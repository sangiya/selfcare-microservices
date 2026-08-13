package com.selfcare.loyalty.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.selfcare.loyalty.domain.TransferChannel;
import com.selfcare.loyalty.service.LoyaltyService;
import com.selfcare.loyalty.web.dto.BalanceResponse;
import com.selfcare.loyalty.web.dto.DonateRequest;
import com.selfcare.loyalty.web.dto.HistoryEntryResponse;
import com.selfcare.loyalty.web.dto.RegisterRequest;
import com.selfcare.loyalty.web.dto.RegisterResponse;
import com.selfcare.loyalty.web.dto.TransferRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class LoyaltyControllerTest {

    private final LoyaltyService loyaltyService = org.mockito.Mockito.mock(LoyaltyService.class);
    private final LoyaltyController controller = new LoyaltyController(loyaltyService);

    @Test
    void getBalance_wrapsTheServiceResponse() {
        when(loyaltyService.getBalance("912345678V", "94771234567"))
                .thenReturn(new BalanceResponse(new BigDecimal("100"), new BigDecimal("80")));

        var response = controller.getBalance("912345678V", "94771234567");

        assertThat(response.success()).isTrue();
        assertThat(response.data().currentBalance()).isEqualByComparingTo("100");
    }

    @Test
    void register_wrapsTheServiceResponse() {
        RegisterRequest request = new RegisterRequest("912345678V", "94771234567", "NIC", "912345678V",
                "Test User", "Colombo", "test@example.com");
        when(loyaltyService.register(request)).thenReturn(new RegisterResponse("PIN_SENT", "TX-001"));

        var response = controller.register(request);

        assertThat(response.data().status()).isEqualTo("PIN_SENT");
        assertThat(response.data().pinTransactionRef()).isEqualTo("TX-001");
    }

    @Test
    void transfer_delegatesAndReturnsSuccessEnvelope() {
        TransferRequest request = new TransferRequest(
                "912345678V", "94771234567", TransferChannel.MOBILE, "94779999999", new BigDecimal("25.00"));

        var response = controller.transfer(request);

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isNull();
        verify(loyaltyService).transfer("912345678V", "94771234567", TransferChannel.MOBILE, "94779999999", new BigDecimal("25.00"));
    }

    @Test
    void donate_delegatesAndReturnsSuccessEnvelope() {
        DonateRequest request = new DonateRequest("912345678V", "94771234567", "charity", new BigDecimal("15.00"));

        var response = controller.donate(request);

        assertThat(response.success()).isTrue();
        verify(loyaltyService).donate("912345678V", "94771234567", "charity", new BigDecimal("15.00"));
    }

    @Test
    void getHistory_wrapsTheServiceResponse() {
        when(loyaltyService.getHistory("912345678V", "94771234567", 10))
                .thenReturn(List.of(new HistoryEntryResponse("TX-1", "EARN", "Store", new BigDecimal("25.00"), "2026-08-10")));

        var response = controller.getHistory("912345678V", "94771234567", 10);

        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().transactionSerial()).isEqualTo("TX-1");
    }

    @Test
    void getRecentActivity_wrapsTheServiceResponse() {
        when(loyaltyService.getRecentActivity("94771234567", 0, 20)).thenReturn(List.of());

        var response = controller.getRecentActivity("94771234567", 0, 20);

        assertThat(response.success()).isTrue();
        verify(loyaltyService).getRecentActivity("94771234567", 0, 20);
    }
}
