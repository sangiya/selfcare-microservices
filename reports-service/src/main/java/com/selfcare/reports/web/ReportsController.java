package com.selfcare.reports.web;

import com.selfcare.platform.common.tenant.TenantContext;
import com.selfcare.platform.common.web.ApiResponse;
import com.selfcare.platform.common.web.NotFoundException;
import com.selfcare.reports.domain.ReportRequest;
import com.selfcare.reports.domain.ReportType;
import com.selfcare.reports.repository.ReportRequestRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * STARTER MODULE. Only the "submit request / check status" shell is implemented; the actual
 * per-{@link ReportType} generation logic (querying CDR/ClickHouse, building the export file)
 * is a TODO per controller -- see README-TODO.md. This intentionally does NOT try to guess
 * the real query logic for a 3,668-line legacy controller.
 */
@RestController
@RequestMapping("/api/v1/reports")
@Validated
@Tag(name = "Reports & CDR", description = "STARTER -- Doc 5 pilot domain 2 of 4; see README-TODO.md")
public class ReportsController {

    private final ReportRequestRepository repository;

    public ReportsController(ReportRequestRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<ReportRequest>> submitReport(
            @RequestParam @NotBlank String subscriberMsisdn,
            @RequestParam @NotNull ReportType reportType,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        ReportRequest request = new ReportRequest();
        request.setTenantId(TenantContext.get());
        request.setSubscriberMsisdn(subscriberMsisdn);
        request.setReportType(reportType);
        request.setFromDate(fromDate);
        request.setToDate(toDate);
        // TODO: publish a "report.requested" Kafka event for an async worker to pick up and
        // populate resultLocation, instead of leaving every request PENDING forever.
        ReportRequest saved = repository.save(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(saved));
    }

    @GetMapping("/requests/{id}")
    public ApiResponse<ReportRequest> getStatus(@PathVariable Long id) {
        ReportRequest request = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("No report request with id=" + id));
        return ApiResponse.ok(request);
    }

    @GetMapping("/requests")
    public ApiResponse<List<ReportRequest>> listForSubscriber(@RequestParam @NotBlank String subscriberMsisdn) {
        return ApiResponse.ok(repository.findBySubscriberMsisdnOrderByCreatedAtDesc(subscriberMsisdn));
    }
}
