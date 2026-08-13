package com.selfcare.config.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.selfcare.config.domain.LayoutDocument;
import com.selfcare.config.domain.TenantConfig;
import com.selfcare.config.service.TenantConfigService;
import com.selfcare.config.web.dto.LayoutDocumentDto;
import com.selfcare.config.web.dto.TenantConfigDto;
import com.selfcare.platform.common.web.BadRequestException;
import com.selfcare.platform.common.web.ApiResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantConfigControllerTest {

    @Mock
    private TenantConfigService tenantConfigService;

    private TenantConfigController controller;

    @BeforeEach
    void setUp() {
        controller = new TenantConfigController(tenantConfigService);
    }

    @Test
    void getTenantConfig_mapsDomainToDto() {
        when(tenantConfigService.getTenantConfig("acme-telecom")).thenReturn(tenantConfig("acme-telecom"));

        ApiResponse<TenantConfigDto> response = controller.getTenantConfig("acme-telecom");

        assertThat(response.success()).isTrue();
        assertThat(response.data().getTenantId()).isEqualTo("acme-telecom");
        assertThat(response.data().getOperatorDisplayName()).isEqualTo("Acme Telecom");
    }

    @Test
    void resolve_usesHostWhenPresent() {
        when(tenantConfigService.resolveByHost("selfcare.acme.test")).thenReturn(tenantConfig("acme-telecom"));

        ApiResponse<TenantConfigDto> response = controller.resolve("selfcare.acme.test", "ignored");

        assertThat(response.data().getTenantId()).isEqualTo("acme-telecom");
        verify(tenantConfigService).resolveByHost("selfcare.acme.test");
    }

    @Test
    void resolve_usesAppFlavorWhenHostMissing() {
        when(tenantConfigService.resolveByAppFlavor("acme-android")).thenReturn(tenantConfig("acme-telecom"));

        ApiResponse<TenantConfigDto> response = controller.resolve(" ", "acme-android");

        assertThat(response.data().getTenantId()).isEqualTo("acme-telecom");
        verify(tenantConfigService).resolveByAppFlavor("acme-android");
    }

    @Test
    void resolve_rejectsWhenNeitherHostNorAppFlavorProvided() {
        assertThatThrownBy(() -> controller.resolve(null, " "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Provide either 'host' or 'appFlavorId'");
    }

    @Test
    void getLayout_mapsDomainToDto() {
        when(tenantConfigService.getLayout("acme-telecom", "dashboard"))
                .thenReturn(layoutDocument("acme-telecom", "dashboard"));

        ApiResponse<LayoutDocumentDto> response = controller.getLayout("acme-telecom", "dashboard");

        assertThat(response.success()).isTrue();
        assertThat(response.data().getTenantId()).isEqualTo("acme-telecom");
        assertThat(response.data().getScreenKey()).isEqualTo("dashboard");
        assertThat(response.data().getWidgets()).hasSize(1);
    }

    @Test
    void upsertTenantConfig_overridesTenantIdFromPathBeforeSaving() {
        when(tenantConfigService.upsertTenantConfig(any(TenantConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));
        TenantConfigDto request = new TenantConfigDto();
        request.setTenantId("wrong-value");
        request.setOperatorDisplayName("Acme Telecom");

        ApiResponse<TenantConfigDto> response = controller.upsertTenantConfig("acme-telecom", request);

        ArgumentCaptor<TenantConfig> captor = ArgumentCaptor.forClass(TenantConfig.class);
        verify(tenantConfigService).upsertTenantConfig(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo("acme-telecom");
        assertThat(response.data().getTenantId()).isEqualTo("acme-telecom");
    }

    @Test
    void upsertLayout_overridesPathValuesBeforeSaving() {
        when(tenantConfigService.upsertLayout(any(LayoutDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        LayoutDocumentDto request = new LayoutDocumentDto();
        request.setTenantId("wrong-tenant");
        request.setScreenKey("wrong-screen");

        ApiResponse<LayoutDocumentDto> response = controller.upsertLayout("acme-telecom", "dashboard", request);

        ArgumentCaptor<LayoutDocument> captor = ArgumentCaptor.forClass(LayoutDocument.class);
        verify(tenantConfigService).upsertLayout(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo("acme-telecom");
        assertThat(captor.getValue().getScreenKey()).isEqualTo("dashboard");
        assertThat(response.data().getTenantId()).isEqualTo("acme-telecom");
        assertThat(response.data().getScreenKey()).isEqualTo("dashboard");
    }

    private static TenantConfig tenantConfig(String tenantId) {
        TenantConfig.ThemeConfig theme = new TenantConfig.ThemeConfig();
        theme.setPrimaryColor("#0055AA");
        theme.setAppDisplayName("Acme Selfcare");

        TenantConfig config = new TenantConfig();
        config.setTenantId(tenantId);
        config.setOperatorDisplayName("Acme Telecom");
        config.setHostAliases(List.of("selfcare.acme.test"));
        config.setAppFlavorIds(List.of("acme-android"));
        config.setTheme(theme);
        config.setApiAdapterBindings(Map.of("loyalty-core", "mife"));
        config.setEnabledFeatureFlags(List.of("loyalty-partner-transfer-flysmiles"));
        config.setActive(true);
        return config;
    }

    private static LayoutDocument layoutDocument(String tenantId, String screenKey) {
        LayoutDocument.Widget widget = new LayoutDocument.Widget();
        widget.setWidgetId("loyalty-summary");
        widget.setType("balance-card");
        widget.setOrder(1);
        widget.setEnabled(true);

        LayoutDocument layout = new LayoutDocument();
        layout.setTenantId(tenantId);
        layout.setScreenKey(screenKey);
        layout.setWidgets(List.of(widget));
        return layout;
    }
}
