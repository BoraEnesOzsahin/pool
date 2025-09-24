package com.ayrotek.pool.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "f2pool")
public class F2PoolProperties {
/** Base url like https://api.f2pool.com/v2 */
private String baseUrl;
/** API token placed in header F2P-API-SECRET */
private String apiSecret;


public String getBaseUrl() { return baseUrl; }
public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
public String getApiSecret() { return apiSecret; }
public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }
}