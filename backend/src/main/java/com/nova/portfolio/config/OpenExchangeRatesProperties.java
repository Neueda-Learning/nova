package com.nova.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "openexchangerates")
public class OpenExchangeRatesProperties {

    private String baseUrl = "https://openexchangerates.org/api";
    private String appId;
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int v) { this.connectTimeoutMs = v; }

    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int v) { this.readTimeoutMs = v; }
}

