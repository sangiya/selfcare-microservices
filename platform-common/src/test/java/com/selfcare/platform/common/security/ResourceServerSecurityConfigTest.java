package com.selfcare.platform.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer.AuthorizedUrl;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;

class ResourceServerSecurityConfigTest {

    @Test
    void outerConfigurationClassIsInstantiableForSpring() {
        assertThat(new ResourceServerSecurityConfig()).isNotNull();
    }

    @Test
    void jwtEnforcedConfig_buildsAProtectedFilterChainAndDecoder() {
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
    void jwtEnforcedConfig_executesItsHiddenConfigurerLambdas() throws Exception {
        ResourceServerSecurityConfig.JwtEnforcedConfig config = new ResourceServerSecurityConfig.JwtEnforcedConfig();

        @SuppressWarnings("unchecked")
        SessionManagementConfigurer<HttpSecurity> sessionManagement =
                mock(SessionManagementConfigurer.class, withSettings().defaultAnswer(Answers.RETURNS_SELF));
        invokeHidden(config, "lambda$filterChain$0", SessionManagementConfigurer.class, sessionManagement);
        verify(sessionManagement).sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        AuthorizationManagerRequestMatcherRegistry registry = mock(AuthorizationManagerRequestMatcherRegistry.class);
        AuthorizedUrl actuator = mock(AuthorizedUrl.class, withSettings().defaultAnswer(Answers.RETURNS_SELF));
        AuthorizedUrl docs = mock(AuthorizedUrl.class, withSettings().defaultAnswer(Answers.RETURNS_SELF));
        AuthorizedUrl anyRequest = mock(AuthorizedUrl.class, withSettings().defaultAnswer(Answers.RETURNS_SELF));
        when(registry.requestMatchers("/actuator/health/**", "/actuator/prometheus", "/actuator/info")).thenReturn(actuator);
        when(registry.requestMatchers("/v3/api-docs/**", "/swagger-ui/**")).thenReturn(docs);
        when(registry.anyRequest()).thenReturn(anyRequest);
        when(actuator.permitAll()).thenReturn(registry);
        when(docs.permitAll()).thenReturn(registry);
        when(anyRequest.authenticated()).thenReturn(registry);
        invokeHidden(config, "lambda$filterChain$1", AuthorizationManagerRequestMatcherRegistry.class, registry);
        verify(actuator).permitAll();
        verify(docs).permitAll();
        verify(anyRequest).authenticated();

        @SuppressWarnings("unchecked")
        OAuth2ResourceServerConfigurer<HttpSecurity> oauth2 =
                mock(OAuth2ResourceServerConfigurer.class, withSettings().defaultAnswer(Answers.RETURNS_SELF));
        invokeHidden(config, "lambda$filterChain$2", OAuth2ResourceServerConfigurer.class, oauth2);
        verify(oauth2).jwt(any());
    }

    @Test
    void permitAllLocalDevConfig_buildsAnOpenFilterChain() {
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

    @Test
    void permitAllLocalDevConfig_executesItsHiddenConfigurerLambdas() throws Exception {
        ResourceServerSecurityConfig.PermitAllLocalDevConfig config =
                new ResourceServerSecurityConfig.PermitAllLocalDevConfig();

        @SuppressWarnings("unchecked")
        SessionManagementConfigurer<HttpSecurity> sessionManagement =
                mock(SessionManagementConfigurer.class, withSettings().defaultAnswer(Answers.RETURNS_SELF));
        invokeHidden(config, "lambda$filterChain$0", SessionManagementConfigurer.class, sessionManagement);
        verify(sessionManagement).sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        AuthorizationManagerRequestMatcherRegistry registry = mock(AuthorizationManagerRequestMatcherRegistry.class);
        AuthorizedUrl anyRequest = mock(AuthorizedUrl.class, withSettings().defaultAnswer(Answers.RETURNS_SELF));
        when(registry.anyRequest()).thenReturn(anyRequest);
        when(anyRequest.permitAll()).thenReturn(registry);
        invokeHidden(config, "lambda$filterChain$1", AuthorizationManagerRequestMatcherRegistry.class, registry);
        verify(anyRequest).permitAll();
    }

    private static void invokeHidden(Object target, String methodName, Class<?> argumentType, Object argument) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, argumentType);
        method.setAccessible(true);
        method.invoke(target, argument);
    }
}
