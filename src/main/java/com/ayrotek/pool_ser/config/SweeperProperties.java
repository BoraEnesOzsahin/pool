package com.ayrotek.pool_ser.config;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SweeperProperties {

    private final String enabledRaw;
    private final BigDecimal minSweepEth;
    private final long pollSeconds;
    private final long pollMillisConfigured;
    private final String coldAddress;

    public SweeperProperties(
            @Value("${SWEEPER_ENABLED:false}") String enabledRaw,
            @Value("${SWEEPER_MIN_SWEEP_ETH:0.01}") BigDecimal minSweepEth,
            @Value("${SWEEPER_POLL_SECONDS:300}") long pollSeconds,
            @Value("${SWEEPER_POLL_MILLIS:0}") long pollMillisConfigured,
            @Value("${SWEEPER_COLD_ADDRESS:}") String coldAddress) {
        this.enabledRaw = enabledRaw;
        this.minSweepEth = sanitizeMinSweep(minSweepEth);
        this.pollSeconds = pollSeconds > 0 ? pollSeconds : 300L;
        this.pollMillisConfigured = pollMillisConfigured > 0 ? pollMillisConfigured : 0L;
        this.coldAddress = sanitizeAddress(coldAddress);
    }

    public boolean enabled() {
        return StringUtils.hasText(enabledRaw) && "true".equalsIgnoreCase(enabledRaw.trim());
    }

    public BigDecimal minSweepEth() {
        return minSweepEth;
    }

    public String coldAddress() {
        return coldAddress;
    }

    public long pollDelayMillisValue() {
        long millis = pollMillisConfigured > 0 ? pollMillisConfigured : pollSeconds * 1000L;
        if (millis < 1000L) {
            millis = 1000L;
        }
        return millis;
    }

    public String pollDelayString() {
        return Long.toString(pollDelayMillisValue());
    }

    private BigDecimal sanitizeMinSweep(BigDecimal value) {
        if (value == null) {
            return BigDecimal.valueOf(0.01d);
        }
        if (value.signum() < 0) {
            return BigDecimal.ZERO;
        }
        return value;
    }

    private String sanitizeAddress(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        return raw.trim();
    }
}
