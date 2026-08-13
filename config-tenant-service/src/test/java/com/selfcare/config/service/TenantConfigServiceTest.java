package com.selfcare.config.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.selfcare.config.domain.LayoutDocument;
import com.selfcare.config.domain.TenantConfig;
import com.selfcare.config.repository.LayoutRepository;
import com.selfcare.config.repository.TenantConfigRepository;
import com.selfcare.platform.common.web.NotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantConfigServiceTest {

    @Mock
    private TenantConfigRepository tenantConfigRepository;
    @Mock
    private LayoutRepository layoutRepository;

    @Test
    void getTenantConfig_returnsOnlyActiveConfigs() {
        TenantConfig config = new TenantConfig();
        config.setTenantId("acme-telecom");
        config.setActive(true);
        when(tenantConfigRepository.findById("acme-telecom")).thenReturn(Optional.of(config));

        TenantConfigService service = new TenantConfigService(tenantConfigRepository, layoutRepository);

        assertThat(service.getTenantConfig("acme-telecom")).isSameAs(config);
    }

    @Test
    void getTenantConfig_throwsWhenConfigIsMissingOrInactive() {
        TenantConfig inactive = new TenantConfig();
        inactive.setTenantId("acme-telecom");
        inactive.setActive(false);
        when(tenantConfigRepository.findById("acme-telecom")).thenReturn(Optional.of(inactive));

        TenantConfigService service = new TenantConfigService(tenantConfigRepository, layoutRepository);

        assertThatThrownBy(() -> service.getTenantConfig("acme-telecom"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("No active tenant config");
    }

    @Test
    void resolveMethods_throwWhenNoTenantMatches() {
        when(tenantConfigRepository.findByHostAliasesContainingAndActiveTrue("selfcare.acme.test")).thenReturn(Optional.empty());
        when(tenantConfigRepository.findByAppFlavorIdsContainingAndActiveTrue("acme-android")).thenReturn(Optional.empty());
        TenantConfigService service = new TenantConfigService(tenantConfigRepository, layoutRepository);

        assertThatThrownBy(() -> service.resolveByHost("selfcare.acme.test"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("host=selfcare.acme.test");
        assertThatThrownBy(() -> service.resolveByAppFlavor("acme-android"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("appFlavorId=acme-android");
    }

    @Test
    void getLayout_throwsWhenLayoutIsMissing() {
        when(layoutRepository.findByTenantIdAndScreenKey("acme-telecom", "dashboard")).thenReturn(Optional.empty());
        TenantConfigService service = new TenantConfigService(tenantConfigRepository, layoutRepository);

        assertThatThrownBy(() -> service.getLayout("acme-telecom", "dashboard"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("screenKey=dashboard");
    }

    @Test
    void upsertTenantConfig_delegatesToTheRepository() {
        TenantConfig config = new TenantConfig();
        config.setTenantId("acme-telecom");
        when(tenantConfigRepository.save(config)).thenReturn(config);
        TenantConfigService service = new TenantConfigService(tenantConfigRepository, layoutRepository);

        assertThat(service.upsertTenantConfig(config)).isSameAs(config);
    }

    @Test
    void upsertLayout_incrementsTheVersionBeforeSaving() {
        LayoutDocument layout = new LayoutDocument();
        layout.setTenantId("acme-telecom");
        layout.setScreenKey("dashboard");
        layout.setWidgets(List.of());
        layout.setVersion(2);
        when(layoutRepository.save(any(LayoutDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        TenantConfigService service = new TenantConfigService(tenantConfigRepository, layoutRepository);

        LayoutDocument saved = service.upsertLayout(layout);

        ArgumentCaptor<LayoutDocument> captor = ArgumentCaptor.forClass(LayoutDocument.class);
        verify(layoutRepository).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(3);
        assertThat(saved.getVersion()).isEqualTo(3);
    }
}
