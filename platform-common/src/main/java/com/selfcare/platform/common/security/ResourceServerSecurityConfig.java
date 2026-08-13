package com.selfcare.platform.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Every microservice validates the same JWT issued by the existing Auth Service (Doc 1 sec
 * 2.1) rather than re-implementing login — this config wires each service up as an OAuth2
 * resource server against that Auth Service's JWK Set, plus the standard set of endpoints
 * (actuator health/prometheus) that must stay open for k8s probes and scraping.
 *
 * <p>Set {@code platform.security.jwk-set-uri} (env var {@code AUTH_SERVICE_JWK_URI}) per
 * environment (dev/QA/staging/prod, and per operator) via the Helm values overlay; it must
 * never be hard-coded to production here.
 *
 * <p>If it's left unset, {@link PermitAllLocalDevConfig} takes over instead of failing to
 * start — this mirrors {@code NoOpCustomerValidationClient}'s dev-only fallback pattern and
 * is ONLY safe for local docker-compose development against a stubbed/absent Auth Service.
 * Real environments must always set {@code AUTH_SERVICE_JWK_URI}; every request is completely
 * unauthenticated while this fallback is active, and it logs a loud warning on startup so
 * this is never silently true anywhere it matters.
 *
 * <p>Both variants build a servlet {@link SecurityFilterChain} from {@link HttpSecurity}, which
 * only exists in a servlet web context. Without the condition below, any non-web context — such
 * as a {@code WebEnvironment.NONE} repository test — fails to start on a missing HttpSecurity
 * bean rather than simply running without a filter chain it has no use for.
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ResourceServerSecurityConfig {

    @Configuration
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${platform.security.jwk-set-uri:}')")
    static class JwtEnforcedConfig {

        @Value("${platform.security.jwk-set-uri}")
        private String jwkSetUri;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable()) // stateless JWT APIs behind the BFF; CSRF not applicable
                    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/actuator/health/**", "/actuator/prometheus", "/actuator/info").permitAll()
                            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
                            .anyRequest().authenticated())
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
            return http.build();
        }

        @Bean
        public JwtDecoder jwtDecoder() {
            return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        }
    }

    @Configuration
    @ConditionalOnExpression("!T(org.springframework.util.StringUtils).hasText('${platform.security.jwk-set-uri:}')")
    static class PermitAllLocalDevConfig {

        private static final Logger log = LoggerFactory.getLogger(PermitAllLocalDevConfig.class);

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            log.warn("!!! platform.security.jwk-set-uri (AUTH_SERVICE_JWK_URI) is NOT SET. Every endpoint "
                    + "on this service is UNAUTHENTICATED (permit-all). This is only acceptable for local "
                    + "docker-compose development against a stubbed/absent Auth Service -- every real "
                    + "environment must set AUTH_SERVICE_JWK_URI. If you are seeing this warning outside "
                    + "local dev, fix the deployment config immediately.");
            http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }
}
