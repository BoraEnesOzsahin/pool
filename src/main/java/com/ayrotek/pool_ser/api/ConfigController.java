package com.ayrotek.pool_ser.api;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.crypto.Credentials;

import com.ayrotek.pool_ser.api.dto.SweeperConfigDto;
import com.ayrotek.pool_ser.config.SweeperProperties;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final Credentials credentials;
    private final Environment environment;
    private final SweeperProperties sweeperProperties;

    public ConfigController(Credentials credentials, Environment environment, SweeperProperties sweeperProperties) {
        this.credentials = credentials;
        this.environment = environment;
        this.sweeperProperties = sweeperProperties;
    }

    @GetMapping
    public SweeperConfigDto config() {
        return new SweeperConfigDto(
                credentials.getAddress(),
                sweeperProperties.coldAddress(),
                sweeperProperties.enabled(),
                sweeperProperties.pollDelayMillisValue(),
                sweeperProperties.minSweepEth().doubleValue(),
                environment.getProperty("SWEEPER_MAX_GAS_GWEI", Double.class, 0d),
                environment.getProperty("SWEEPER_RECEIPT_POLL_ENABLED", Boolean.class, Boolean.FALSE),
                environment.getProperty("SWEEPER_RECEIPT_POLL_MILLIS", Long.class, 0L),
                environment.getProperty("SWEEPER_RECEIPT_MAX_RETRIES", Integer.class, 0)
        );
    }
}
