package com.ayrotek.dogecoin_pool.doge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "electrs.doge")
public class ElectrsDogecoinProperties {

    /**
     * Base URL for an electrs-compatible REST API.
     * Example: https://doge-electrs-testnet-demo.qed.me
     */
    private String baseUrl = "https://doge-electrs-testnet-demo.qed.me";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
