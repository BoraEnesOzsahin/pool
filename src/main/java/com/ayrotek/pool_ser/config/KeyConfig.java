package com.ayrotek.pool_ser.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.web3j.crypto.Credentials;

@Configuration
public class KeyConfig {

    private static final Logger log = LoggerFactory.getLogger(KeyConfig.class);

    @Bean
    public Credentials sweeperCredentials(Environment environment) {
        final String key = environment.getProperty("SWEEPER_HOT_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("SWEEPER_HOT_KEY environment variable is required");
        }
        try {
            return Credentials.create(key.trim());
        } catch (RuntimeException ex) {
            log.error("Failed to create credentials from SWEEPER_HOT_KEY", ex);
            throw new IllegalStateException("Invalid SWEEPER_HOT_KEY provided", ex);
        }
    }
}
