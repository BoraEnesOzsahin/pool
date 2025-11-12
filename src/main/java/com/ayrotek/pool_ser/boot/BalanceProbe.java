package com.ayrotek.pool_ser.boot;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.utils.Convert;

@Component
public class BalanceProbe implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BalanceProbe.class);

    private final Web3j web3j;
    private final Credentials credentials;

    public BalanceProbe(Web3j web3j, Credentials credentials) {
        this.web3j = web3j;
        this.credentials = credentials;
    }

    @Override
    public void run(ApplicationArguments args) {
        final String address = credentials.getAddress();
        log.info("Hot wallet address: {}", address);

        try {
            EthGetBalance response = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
            if (response == null || response.hasError()) {
                String errorMessage = response != null && response.getError() != null
                        ? response.getError().getMessage()
                        : "Unknown error";
                log.error("Failed to fetch balance for {}: {}", address, errorMessage);
                throw new IllegalStateException("Unable to retrieve ETH balance");
            }

            BigInteger balanceWei = response.getBalance();
            if (balanceWei == null) {
                log.error("Balance response for {} is null", address);
                throw new IllegalStateException("ETH balance is unavailable");
            }

            BigDecimal balanceEth = Convert.fromWei(new BigDecimal(balanceWei), Convert.Unit.ETHER);
            log.info("Hot wallet balance: {} Wei ({} ETH)", balanceWei, balanceEth);
        } catch (IOException ex) {
            log.error("Error while calling eth_getBalance for {}: {}", address, ex.getMessage());
            throw new IllegalStateException("RPC call for ETH balance failed", ex);
        }
    }
}
