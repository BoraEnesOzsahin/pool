package com.ayrotek.pool_ser.service;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthChainId;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.utils.Numeric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Service
public class SweepPlanner {

    private static final Logger log = LoggerFactory.getLogger(SweepPlanner.class);

    private static final BigInteger GAS_LIMIT = BigInteger.valueOf(21_000L);
    private static final BigInteger RESERVE_WEI = BigInteger.valueOf(1_000_000L);
    private static final BigInteger ONE_GWEI = BigInteger.valueOf(1_000_000_000L);

    private final Web3j web3j;
    private final Web3jService web3jService;
    private final NonceService nonceService;

    public SweepPlanner(Web3j web3j, NonceService nonceService) {
        this.web3j = web3j;
        this.web3jService = extractService(web3j);
        this.nonceService = nonceService;
    }

    public Plan plan(String fromAddress, String toAddress) {
        if (!StringUtils.hasText(fromAddress)) {
            throw new IllegalArgumentException("fromAddress must not be blank");
        }
        if (!StringUtils.hasText(toAddress)) {
            throw new IllegalArgumentException("toAddress must not be blank");
        }

        final String from = fromAddress.trim();
        final String to = toAddress.trim();

        try {
            BigInteger chainId = fetchChainId();
            BigInteger nonce = nonceService.nextNonce(from);
            BigInteger balance = fetchBalance(from);
            BigInteger priorityFee = fetchPriorityFee();
            Optional<BigInteger> baseFeeHint = fetchBaseFeeHint();
            BigInteger gasPriceFallback = fetchGasPrice();

            BigInteger maxFeePerGas = determineMaxFeePerGas(baseFeeHint, priorityFee, gasPriceFallback);
            BigInteger effectiveFeePerGas = maxFeePerGas;
            BigInteger gasCost = GAS_LIMIT.multiply(effectiveFeePerGas);
            BigInteger sweepReserve = RESERVE_WEI;
            BigInteger sweepAmountRaw = balance.subtract(gasCost).subtract(sweepReserve);
            boolean sweepable = sweepAmountRaw.compareTo(BigInteger.ZERO) > 0;
            BigInteger sweepAmount = sweepable ? sweepAmountRaw : BigInteger.ZERO;

            return new Plan(
                    chainId,
                    nonce,
                    balance,
                    GAS_LIMIT,
                    priorityFee,
                    maxFeePerGas,
                    effectiveFeePerGas,
                    gasCost,
                    sweepReserve,
                    sweepAmount,
                    from,
                    to,
                    sweepable);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to plan sweep due to RPC error", ex);
        }
    }

    private BigInteger fetchChainId() throws IOException {
        EthChainId response = web3j.ethChainId().send();
        EthChainId verified = requireResponse("eth_chainId", response);
        BigInteger chainId = verified.getChainId();
        if (chainId == null) {
            throw new IllegalStateException("eth_chainId returned null chain id");
        }
        return chainId;
    }

    private BigInteger fetchBalance(String fromAddress) throws IOException {
        EthGetBalance response = web3j.ethGetBalance(fromAddress, DefaultBlockParameterName.PENDING).send();
        EthGetBalance verified = requireResponse("eth_getBalance", response);
        BigInteger balance = verified.getBalance();
        if (balance == null) {
            throw new IllegalStateException("eth_getBalance returned null balance");
        }
        return balance;
    }

    private BigInteger fetchPriorityFee() throws IOException {
        Request<?, EthGasPrice> request = new Request<>(
                "eth_maxPriorityFeePerGas",
                Collections.emptyList(),
                web3jService,
                EthGasPrice.class);
        EthGasPrice response = request.send();
        if (response == null || response.hasError()) {
            log.debug("eth_maxPriorityFeePerGas unavailable, using 1 gwei fallback");
            return ONE_GWEI;
        }
        BigInteger value = response.getGasPrice();
        if (value == null || value.signum() <= 0) {
            log.debug("eth_maxPriorityFeePerGas returned non-positive value, using 1 gwei fallback");
            return ONE_GWEI;
        }
        return value;
    }

    private Optional<BigInteger> fetchBaseFeeHint() throws IOException {
        try {
            List<Object> params = List.of(
                    Numeric.encodeQuantity(BigInteger.ONE),
                    DefaultBlockParameterName.LATEST.getValue(),
                    List.of(50d));
            Request<?, FeeHistoryResponse> request = new Request<>(
                    "eth_feeHistory",
                    params,
                    web3jService,
                    FeeHistoryResponse.class);
            FeeHistoryResponse response = request.send();
            if (response == null || response.hasError()) {
                log.debug("eth_feeHistory unavailable, will fallback to eth_gasPrice");
                return Optional.empty();
            }
            FeeHistoryPayload history = response.feeHistory();
            if (history == null) {
                log.debug("eth_feeHistory returned empty payload");
                return Optional.empty();
            }
            List<String> baseFeePerGas = history.baseFeePerGas();
            if (baseFeePerGas == null || baseFeePerGas.size() < 2) {
                log.debug("eth_feeHistory returned insufficient base fee data");
                return Optional.empty();
            }
            String nextBaseFeeHex = baseFeePerGas.get(baseFeePerGas.size() - 1);
            if (nextBaseFeeHex == null) {
                log.debug("eth_feeHistory base fee hint was null or non-positive");
                return Optional.empty();
            }
            BigInteger nextBaseFee = Numeric.decodeQuantity(nextBaseFeeHex);
            if (nextBaseFee == null || nextBaseFee.signum() <= 0) {
                log.debug("eth_feeHistory base fee hint was null or non-positive");
                return Optional.empty();
            }
            return Optional.of(nextBaseFee);
        } catch (UnsupportedOperationException ex) {
            log.debug("eth_feeHistory unsupported, will fallback to eth_gasPrice");
            return Optional.empty();
        }
    }

    private BigInteger fetchGasPrice() throws IOException {
        EthGasPrice response = web3j.ethGasPrice().send();
        EthGasPrice verified = requireResponse("eth_gasPrice", response);
        BigInteger gasPrice = verified.getGasPrice();
        if (gasPrice == null || gasPrice.signum() <= 0) {
            throw new IllegalStateException("eth_gasPrice returned invalid value");
        }
        return gasPrice;
    }

    private BigInteger determineMaxFeePerGas(Optional<BigInteger> baseFeeHint, BigInteger priorityFee,
            BigInteger gasPriceFallback) {
        return baseFeeHint
                .map(baseFee -> baseFee.add(priorityFee.multiply(BigInteger.TWO)))
                .orElse(gasPriceFallback);
    }

    private <T extends Response<?>> T requireResponse(String callName, T response) {
        if (response == null) {
            throw new IllegalStateException(callName + " returned null response");
        }
        if (response.hasError()) {
            String message = response.getError() != null ? response.getError().getMessage() : "Unknown RPC error";
            throw new IllegalStateException(callName + " failed: " + message);
        }
        return response;
    }

    private Web3jService extractService(Web3j web3j) {
        if (web3j instanceof JsonRpc2_0Web3j jsonRpc) {
            try {
                Field field = JsonRpc2_0Web3j.class.getDeclaredField("web3jService");
                field.setAccessible(true);
                Object service = field.get(jsonRpc);
                if (service instanceof Web3jService web3jServiceInstance) {
                    return web3jServiceInstance;
                }
                throw new IllegalStateException("Unsupported Web3jService implementation: "
                        + (service != null ? service.getClass().getName() : "null"));
            } catch (NoSuchFieldException | IllegalAccessException ex) {
                throw new IllegalStateException("Unable to access Web3jService", ex);
            }
        }
        throw new IllegalStateException("Unsupported Web3j implementation: " + web3j.getClass().getName());
    }

    public record Plan(
            BigInteger chainId,
            BigInteger nonce,
            BigInteger balanceWei,
            BigInteger gasLimit,
            BigInteger priorityFeeWei,
            BigInteger maxFeeWei,
            BigInteger effectiveFeePerGasWei,
            BigInteger gasCostWei,
            BigInteger reserveWei,
            BigInteger sweepAmountWei,
            String from,
            String to,
            boolean sweepable) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class FeeHistoryResponse extends Response<FeeHistoryPayload> {
        FeeHistoryPayload feeHistory() {
            return getResult();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class FeeHistoryPayload {
        @JsonProperty("baseFeePerGas")
        private List<String> baseFeePerGas;

        List<String> baseFeePerGas() {
            return baseFeePerGas;
        }
    }
}
