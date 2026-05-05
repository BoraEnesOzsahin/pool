package com.ayrotek.dogecoin_pool.doge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "tatum.doge")
public class TatumDogecoinProperties {

    /**
     * Base URL for Tatum REST API.
     * Typically "https://api.tatum.io".
     */
    private String baseUrl = "https://api.tatum.io";

    /**
     * Tatum API key bound to Dogecoin TESTNET.
     */
    private String apiKey;

    /**
     * Testnet HOT wallet address + private key (swept FROM).
     * TESTNET ONLY.
     */
    private String hotAddress;
    private String hotPrivateKey;

    /**
     * Testnet COLD wallet address (swept TO).
     */
    private String coldAddress;

    /**
     * Minimal sweep threshold in DOGE.
     * If balance < minSweepDoge, skip.
     */
    private double minSweepDoge = 10.0;

    /**
     * Reserve in DOGE to keep on HOT address after sweeping.
     */
    private double reserveDoge = 1.0;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getHotAddress() {
        return hotAddress;
    }

    public void setHotAddress(String hotAddress) {
        this.hotAddress = hotAddress;
    }

    public String getHotPrivateKey() {
        return hotPrivateKey;
    }

    public void setHotPrivateKey(String hotPrivateKey) {
        this.hotPrivateKey = hotPrivateKey;
    }

    public String getColdAddress() {
        return coldAddress;
    }

    public void setColdAddress(String coldAddress) {
        this.coldAddress = coldAddress;
    }

    public double getMinSweepDoge() {
        return minSweepDoge;
    }

    public void setMinSweepDoge(double minSweepDoge) {
        this.minSweepDoge = minSweepDoge;
    }

    public double getReserveDoge() {
        return reserveDoge;
    }

    public void setReserveDoge(double reserveDoge) {
        this.reserveDoge = reserveDoge;
    }
}
