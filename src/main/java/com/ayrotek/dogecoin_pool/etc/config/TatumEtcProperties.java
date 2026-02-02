package com.ayrotek.dogecoin_pool.etc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "tatum.etc")
public class TatumEtcProperties {

    /**
     * Base URL for Tatum ETC Testnet API.
     * Default: "https://ethereum-classic-testnet.gateway.tatum.io"
     */
    private String baseUrl = "https://ethereum-classic-testnet.gateway.tatum.io";

    /**
     * Tatum API key for Ethereum Classic TESTNET.
     */
    private String apiKey;

    /**
     * Testnet HOT wallet address (swept FROM).
     * TESTNET ONLY - should be a valid ETC testnet address (0x...).
     */
    private String hotAddress;

    /**
     * Testnet HOT wallet private key.
     * TESTNET ONLY.
     */
    private String hotPrivateKey;

    /**
     * Testnet COLD wallet address (swept TO).
     * Should be a valid ETC testnet address (0x...).
     */
    private String coldAddress;

    /**
     * Minimal sweep threshold in ETC.
     * If balance < minSweepEtc, skip sweeping.
     */
    private double minSweepEtc = 1.0;

    /**
     * Reserve in ETC to keep on HOT address after sweeping.
     * This amount is kept for gas fees.
     */
    private double reserveEtc = 0.5;

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

    public double getMinSweepEtc() {
        return minSweepEtc;
    }

    public void setMinSweepEtc(double minSweepEtc) {
        this.minSweepEtc = minSweepEtc;
    }

    public double getReserveEtc() {
        return reserveEtc;
    }

    public void setReserveEtc(double reserveEtc) {
        this.reserveEtc = reserveEtc;
    }
}
