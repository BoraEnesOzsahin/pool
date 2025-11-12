package com.ayrotek.pool_ser.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
public class Web3Config {

    private final Environment environment;

    public Web3Config(Environment environment) {
        this.environment = environment;
    }

    @Bean(destroyMethod = "shutdown")
    public Web3j web3j() {
        String httpUrl = environment.getProperty("ALCHEMY_HTTP_URL");
        if (!StringUtils.hasText(httpUrl)) {
            throw new IllegalStateException("ALCHEMY_HTTP_URL environment variable must be set");
        }
        return Web3j.build(new HttpService(httpUrl));
    }
}
