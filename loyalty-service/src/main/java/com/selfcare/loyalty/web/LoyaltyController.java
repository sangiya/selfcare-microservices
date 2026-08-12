package com.selfcare.loyalty.web;

import com.selfcare.loyalty.service.LoyaltyService;
import com.selfcare.loyalty.web.dto.ActivityItemResponse;
import com.selfcare.loyalty.web.dto.BalanceResponse;
import com.selfcare.loyalty.web.dto.DonateRequest;
import com.selfcare.loyalty.web.dto.HistoryEntryResponse;
import com.selfcare.loyalty.web.dto.RegisterRequest;
import com.selfcare.loyalty.web.dto.RegisterResponse;
import com.selfcare.loyalty.web.dto.TransferRequest;
import com.selfcare.platform.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Java replacement for {@code scapp/StarPointsController.php} + the relevant slice of
 * {@code api/LoyaltyController.php} (Doc 1 sec 2.3, Doc 5 sec 4). Every endpoint here is one of
 * the legacy {@code actionXxx()} methods, given a typed request/response contract and the
 * standard {@link ApiResponse} envelope instead of the legacy {@code {'success' => ...}} shape.
 */
@RestController
@RequestMapping("/api/v1/loyalty")
@Validated
@Tag(name = "Loyalty & Rewards", description = "Star Points-style loyalty program (Doc 5 pilot domain 1 of 4)")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    public LoyaltyController(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    @GetMapping("/balance")
    @Operation(summary = "Get a subscriber's points balance (legacy: actionGetBalance)")
    public ApiResponse<BalanceResponse> getBalance(
            @RequestParam @NotBlank String nationalId, @RequestParam @NotBlank String msisdn) {
        return ApiResponse.ok(loyaltyService.getBalance(nationalId, msisdn));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a subscriber for the loyalty program (legacy: actionRegister)")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(loyaltyService.register(request));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer points to another subscriber or partner program (legacy: actionTransfer)")
    public ApiResponse<Void> transfer(@Valid @RequestBody TransferRequest request) {
        loyaltyService.transfer(request.nationalId(), request.fromMsisdn(), request.channel(), request.toIdentifier(),
                request.amount());
        return ApiResponse.ok(null);
    }

    @PostMapping("/donate")
    @Operation(summary = "Donate points to a charity alias (legacy: actionDonate)")
    public ApiResponse<Void> donate(@Valid @RequestBody DonateRequest request) {
        loyaltyService.donate(request.nationalId(), request.msisdn(), request.donationAlias(), request.amount());
        return ApiResponse.ok(null);
    }

    @GetMapping("/history")
    @Operation(summary = "Get earn/burn history from the loyalty core system (legacy: actionGetHistory)")
    public ApiResponse<List<HistoryEntryResponse>> getHistory(
            @RequestParam @NotBlank String nationalId,
            @RequestParam @NotBlank String msisdn,
            @RequestParam(defaultValue = "20") int listSize) {
        return ApiResponse.ok(loyaltyService.getHistory(nationalId, msisdn, listSize));
    }

    @GetMapping("/activity")
    @Operation(summary = "Get this service's own request/audit trail for a subscriber (new -- replaces grep-able audit_log)")
    public ApiResponse<List<ActivityItemResponse>> getRecentActivity(
            @RequestParam @NotBlank String msisdn,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(loyaltyService.getRecentActivity(msisdn, page, size));
    }
}
