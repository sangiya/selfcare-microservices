package com.selfcare.loyalty.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from {@code loyalty.mife.*}. The counter alias/auth pairs are the Java replacement for
 * the {@code SP_COUNTER_*_ALIAS} / {@code SP_COUNTER_*_PASS} PHP constants -- those were found
 * defined alongside other plaintext config during the codebase audit (Doc 1 sec 2.3). Here they
 * are read from environment/secret-manager-injected values only; nothing below has a real
 * default, so the service fails fast on startup if secrets aren't wired, rather than silently
 * running with a blank credential.
 */
@ConfigurationProperties(prefix = "loyalty.mife")
public class MifeProperties {

    private String baseUrl;
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 8000;

    private Counter balanceCounter = new Counter();
    private Counter transferCounter = new Counter();
    private Counter donateCounter = new Counter();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public Counter getBalanceCounter() {
        return balanceCounter;
    }

    public void setBalanceCounter(Counter balanceCounter) {
        this.balanceCounter = balanceCounter;
    }

    public Counter getTransferCounter() {
        return transferCounter;
    }

    public void setTransferCounter(Counter transferCounter) {
        this.transferCounter = transferCounter;
    }

    public Counter getDonateCounter() {
        return donateCounter;
    }

    public void setDonateCounter(Counter donateCounter) {
        this.donateCounter = donateCounter;
    }

    public static class Counter {
        private String alias;
        private String auth;

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public String getAuth() {
            return auth;
        }

        public void setAuth(String auth) {
            this.auth = auth;
        }
    }
}
