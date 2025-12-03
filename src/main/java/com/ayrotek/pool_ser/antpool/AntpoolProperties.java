package com.ayrotek.pool_ser.antpool;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "antpool")
public class AntpoolProperties {

    private static final String DEFAULT_BASE_URL = "https://antpool.com/api/v1";
    private static final String DEFAULT_COIN = "ETH";

    private String userId;
    private String apiKey;
    private String apiSecret;
    private String baseUrl = DEFAULT_BASE_URL;
    private String defaultCoin = DEFAULT_COIN;

    public void assertCredentialsPresent() {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(apiSecret) || !StringUtils.hasText(userId)) {
            throw new IllegalStateException("AntPool credentials are not configured. Set ANTPOOL_USER_ID/API_KEY/API_SECRET.");
        }
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = StringUtils.hasText(baseUrl) ? baseUrl : DEFAULT_BASE_URL;
    }

    public String getDefaultCoin() {
        return defaultCoin;
    }

    public void setDefaultCoin(String defaultCoin) {
        this.defaultCoin = StringUtils.hasText(defaultCoin) ? defaultCoin : DEFAULT_COIN;
    }
}
