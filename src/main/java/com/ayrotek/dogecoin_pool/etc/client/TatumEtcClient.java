package com.ayrotek.dogecoin_pool.etc.client;

import com.ayrotek.dogecoin_pool.etc.config.TatumEtcProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Map;

@Service
public class TatumEtcClient {

    private static final Logger log = LoggerFactory.getLogger(TatumEtcClient.class);

    private final WebClient webClient;
    private final TatumEtcProperties props;

    public TatumEtcClient(TatumEtcProperties props, WebClient.Builder webClientBuilder) {
        this.props = props;
        this.webClient = webClientBuilder
                .baseUrl(props.getBaseUrl())
                .defaultHeader("X-API-Key", props.getApiKey() != null ? props.getApiKey() : "")
                .build();
    }

    /**
     * Get ETC balance for an address on testnet.
     * Returns balance in Wei (smallest unit).
     */
    public Map<String, Object> getAddressBalance(String address) {
        ensureApiKey();
        validateEtcAddress(address);
        
        try {
            log.info("Fetching ETC testnet balance for address: {}", address);
            if (isGatewayBaseUrl()) {
            Map<String, Object> rpcResponse = webClient.post()
                .uri("/")
                .bodyValue(Map.of(
                    "jsonrpc", "2.0",
                    "id", 1,
                    "method", "eth_getBalance",
                    "params", new Object[]{address, "latest"}
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            String hexBalance = rpcResponse == null ? null : String.valueOf(rpcResponse.get("result"));
            BigInteger balanceWei = parseHexWei(hexBalance);
            return Map.of("balance", balanceWei.toString());
            }

            return webClient.get()
                .uri("/v3/ethereum-classic/account/balance/{address}", address)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        } catch (WebClientResponseException wcre) {
            log.error("Tatum ETC balance call failed status={} body={}", 
                    wcre.getStatusCode(), wcre.getResponseBodyAsString());
            throw wcre;
        } catch (RuntimeException ex) {
            log.error("Failed to call Tatum ETC balance for {}", address, ex);
            throw ex;
        }
    }

    /**
     * Get current gas price for ETC testnet.
     */
    public Map<String, Object> getGasPrice() {
        ensureApiKey();
        
        try {
            log.info("Fetching ETC testnet gas price");
            if (isGatewayBaseUrl()) {
            Map<String, Object> rpcResponse = webClient.post()
                .uri("/")
                .bodyValue(Map.of(
                    "jsonrpc", "2.0",
                    "id", 1,
                    "method", "eth_gasPrice",
                    "params", new Object[]{}
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            String hexGasPrice = rpcResponse == null ? null : String.valueOf(rpcResponse.get("result"));
            BigInteger gasPriceWei = parseHexWei(hexGasPrice);
            return Map.of("gasPrice", gasPriceWei.toString());
            }

            return webClient.get()
                .uri("/v3/ethereum-classic/gas")
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        } catch (WebClientResponseException wcre) {
            log.error("Tatum ETC gas price call failed status={} body={}", 
                    wcre.getStatusCode(), wcre.getResponseBodyAsString());
            throw wcre;
        } catch (RuntimeException ex) {
            log.error("Failed to get ETC gas price", ex);
            throw ex;
        }
    }

    /**
     * Validate that the address follows ETC address format.
     */
    private void validateEtcAddress(String address) {
        if (address == null || !address.matches("^0x[a-fA-F0-9]{40}$")) {
            throw new IllegalArgumentException(
                "Invalid ETC address format. Must start with 0x followed by 40 hex characters: " + address
            );
        }
    }

    private void ensureApiKey() {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new IllegalStateException("Tatum ETC apiKey is not configured (tatum.etc.api-key)");
        }
    }

    public BigInteger getGasPriceWei() {
        Map<String, Object> gasPrice = getGasPrice();
        if (gasPrice == null || !gasPrice.containsKey("gasPrice")) {
            return BigInteger.ZERO;
        }
        return new BigInteger(String.valueOf(gasPrice.get("gasPrice")));
    }

    public BigInteger getNonce(String address) {
        ensureApiKey();
        validateEtcAddress(address);

        if (!isGatewayBaseUrl()) {
            throw new UnsupportedOperationException("Non-gateway base URL is not supported for nonce retrieval");
        }

        Map<String, Object> rpcResponse = webClient.post()
                .uri("/")
                .bodyValue(Map.of(
                        "jsonrpc", "2.0",
                        "id", 1,
                        "method", "eth_getTransactionCount",
                        "params", new Object[]{address, "pending"}
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String hexNonce = rpcResponse == null ? null : String.valueOf(rpcResponse.get("result"));
        return parseHexWei(hexNonce);
    }

    public BigInteger getChainId() {
        ensureApiKey();

        if (!isGatewayBaseUrl()) {
            throw new UnsupportedOperationException("Non-gateway base URL is not supported for chainId retrieval");
        }

        Map<String, Object> rpcResponse = webClient.post()
                .uri("/")
                .bodyValue(Map.of(
                        "jsonrpc", "2.0",
                        "id", 1,
                        "method", "eth_chainId",
                        "params", new Object[]{}
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String hexChainId = rpcResponse == null ? null : String.valueOf(rpcResponse.get("result"));
        BigInteger chainId = parseHexWei(hexChainId);
        if (chainId == null || BigInteger.ZERO.equals(chainId)) {
            throw new IllegalStateException("Failed to retrieve chainId from RPC gateway");
        }
        return chainId;
    }

    public TxResult sendTransaction(String fromAddress, String fromPrivateKey, String toAddress, BigInteger valueWei) {
        ensureApiKey();
        validateEtcAddress(fromAddress);
        validateEtcAddress(toAddress);

        if (!isGatewayBaseUrl()) {
            throw new UnsupportedOperationException("Non-gateway base URL is not supported for RPC sendTransaction");
        }

        String normalizedKey = fromPrivateKey.startsWith("0x") ? fromPrivateKey.substring(2) : fromPrivateKey;
        Credentials credentials = Credentials.create(normalizedKey);
        String derivedAddress = credentials.getAddress();
        if (!derivedAddress.equalsIgnoreCase(fromAddress)) {
            log.warn("Provided HOT address {} does not match derived address {} from private key", fromAddress, derivedAddress);
        }

        BigInteger nonce = getNonce(fromAddress);
        BigInteger gasPriceWei = getGasPriceWei();
        BigInteger gasLimit = BigInteger.valueOf(21000);
        BigInteger chainId = getChainId();

        RawTransaction rawTx = RawTransaction.createEtherTransaction(
                nonce,
                gasPriceWei,
                gasLimit,
                toAddress,
                valueWei
        );

        byte[] signedMessage = TransactionEncoder.signMessage(rawTx, chainId.longValue(), credentials);
        String signedHex = Numeric.toHexString(signedMessage);

        Map<String, Object> rpcResponse = webClient.post()
                .uri("/")
                .bodyValue(Map.of(
                        "jsonrpc", "2.0",
                        "id", 1,
                        "method", "eth_sendRawTransaction",
                        "params", new Object[]{signedHex}
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (rpcResponse != null && rpcResponse.containsKey("error")) {
            Object error = rpcResponse.get("error");
            log.error("RPC Error during sendRawTransaction: {}", error);
            throw new IllegalStateException("RPC sendRawTransaction failed: " + error);
        }

        String txHash = rpcResponse == null ? null : String.valueOf(rpcResponse.get("result"));
        if (txHash == null || txHash.isBlank() || "null".equalsIgnoreCase(txHash)) {
            throw new IllegalStateException("RPC sendRawTransaction returned empty tx hash. Response: " + rpcResponse);
        }
        return new TxResult(txHash, gasPriceWei, gasLimit);
    }


    private boolean isGatewayBaseUrl() {
        String baseUrl = props.getBaseUrl();
        return baseUrl != null && baseUrl.contains("gateway.tatum.io");
    }

    private BigInteger parseHexWei(String hexValue) {
        if (hexValue == null || hexValue.isBlank() || "null".equalsIgnoreCase(hexValue)) {
            return BigInteger.ZERO;
        }
        String normalized = hexValue.startsWith("0x") ? hexValue.substring(2) : hexValue;
        if (normalized.isBlank()) {
            return BigInteger.ZERO;
        }
        return new BigInteger(normalized, 16);
    }

    public record TxResult(String txHash, BigInteger gasPriceWei, BigInteger gasLimit) {
    }


    public TatumEtcProperties getProperties() {
        return props;
    }
}
