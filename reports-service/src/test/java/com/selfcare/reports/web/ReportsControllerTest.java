package com.selfcare.reports.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.selfcare.platform.common.tenant.TenantContext;
import com.selfcare.platform.common.web.ApiResponse;
import com.selfcare.platform.common.web.NotFoundException;
import com.selfcare.reports.domain.ReportRequest;
import com.selfcare.reports.domain.ReportStatus;
import com.selfcare.reports.domain.ReportType;
import com.selfcare.reports.repository.ReportRequestRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for the shell that IS implemented -- submit/status/list. Per-{@link ReportType}
 * generation logic is still a TODO (see the class javadoc and README-TODO.md), so there's
 * nothing to unit test there yet; extend this file alongside that work, following
 * loyalty-service's {@code LoyaltyServiceImplTest} pattern.
 */
@ExtendWith(MockitoExtension.class)
class ReportsControllerTest {

    private static final String TENANT = "acme-telecom";
    private static final String MSISDN = "94771234567";

    @Mock
    private ReportRequestRepository repository;

    private ReportsController controller;

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT);
        controller = new ReportsController(repository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void submitReport_savesPendingRequestScopedToCurrentTenant() {
        when(repository.save(any(ReportRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);

        ResponseEntity<ApiResponse<ReportRequest>> response =
                controller.submitReport(MSISDN, ReportType.ACTIVITY_REPORT, from, to);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        ReportRequest saved = response.getBody().data();
        assertThat(saved.getTenantId()).isEqualTo(TENANT);
        assertThat(saved.getSubscriberMsisdn()).isEqualTo(MSISDN);
        assertThat(saved.getReportType()).isEqualTo(ReportType.ACTIVITY_REPORT);
        assertThat(saved.getFromDate()).isEqualTo(from);
        assertThat(saved.getToDate()).isEqualTo(to);
        // Entity default -- nothing has generated the report yet, see the TODO on the async
        // "report.requested" event this controller doesn't publish yet.
        assertThat(saved.getStatus()).isEqualTo(ReportStatus.PENDING);
        verify(repository).save(any(ReportRequest.class));
    }

    @Test
    void getStatus_returnsRequestWhenFound() {
        ReportRequest existing = new ReportRequest();
        existing.setTenantId(TENANT);
        when(repository.findById(42L)).thenReturn(Optional.of(existing));

        ApiResponse<ReportRequest> response = controller.getStatus(42L);

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isSameAs(existing);
    }

    @Test
    void getStatus_throwsNotFoundWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getStatus(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void listForSubscriber_returnsRepositoryResultAsIs() {
        List<ReportRequest> existing = List.of(new ReportRequest(), new ReportRequest());
        when(repository.findBySubscriberMsisdnOrderByCreatedAtDesc(MSISDN)).thenReturn(existing);

        ApiResponse<List<ReportRequest>> response = controller.listForSubscriber(MSISDN);

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo(existing);
    }
}
