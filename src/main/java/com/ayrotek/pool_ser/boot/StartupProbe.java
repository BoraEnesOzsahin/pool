package com.ayrotek.pool_ser.boot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;

@Component
public class StartupProbe implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupProbe.class);

    private final Web3j web3j;

    public StartupProbe(Web3j web3j) {
        this.web3j = web3j;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Web3ClientVersion response = web3j.web3ClientVersion().send();
            if (response == null || response.hasError()) {
                String message = response != null && response.getError() != null
                        ? response.getError().getMessage()
                        : "No response from Alchemy RPC";
                log.error("Alchemy RPC connection failed: {}", message);
                return;
            }

            String clientVersion = response.getWeb3ClientVersion();
            if (!StringUtils.hasText(clientVersion)) {
                log.error("Alchemy RPC connection returned empty client version");
                return;
            }

            log.info("Alchemy client version: {}", clientVersion);
        } catch (Exception ex) {
            log.error("Alchemy RPC connection failed: {}", ex.getMessage());
        }
    }
}
