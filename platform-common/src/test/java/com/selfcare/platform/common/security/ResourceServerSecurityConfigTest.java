package com.selfcare.platform.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;

class ResourceServerSecurityConfigTest {

    @Test
    void jwtEnforcedConfig_buildsAProtectedFilterChainAndDecoder() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class, withSettings().defaultAnswer(Answers.RETURNS_SELF));
        SecurityFilterChain chain = mock(SecurityFilterChain.class);
        doReturn(chain).when(http).build();

        ResourceServerSecurityConfig.JwtEnforcedConfig config = new ResourceServerSecurityConfig.JwtEnforcedConfig();
        ReflectionTestUtils.setField(config, "jwkSetUri", "https://example.test/oauth2/jwks");

        SecurityFilterChain built = config.filterChain(http);
        JwtDecoder decoder = config.jwtDecoder();

        assertThat(built).isSameAs(chain);
        assertThat(decoder).isNotNull();
        verify(http).csrf(any());
        verify(http).sessionManagement(any());
        verify(http).authorizeHttpRequests(any());
        verify(http).oauth2ResourceServer(any());
    }

    @Test
    void permitAllLocalDevConfig_buildsAnOpenFilterChain() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class, withSettings().defaultAnswer(Answers.RETURNS_SELF));
        SecurityFilterChain chain = mock(SecurityFilterChain.class);
        doReturn(chain).when(http).build();

        ResourceServerSecurityConfig.PermitAllLocalDevConfig config =
                new ResourceServerSecurityConfig.PermitAllLocalDevConfig();

        SecurityFilterChain built = config.filterChain(http);

        assertThat(built).isSameAs(chain);
        verify(http).csrf(any());
        verify(http).sessionManagement(any());
        verify(http).authorizeHttpRequests(any());
    }
}
