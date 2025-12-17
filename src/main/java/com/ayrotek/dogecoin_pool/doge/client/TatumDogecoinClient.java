package com.ayrotek.dogecoin_pool.doge.client;

import com.ayrotek.dogecoin_pool.doge.config.TatumDogecoinProperties;
import com.ayrotek.dogecoin_pool.doge.dto.DogeBalanceDto;
import com.ayrotek.dogecoin_pool.doge.util.DogecoinAddressValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class TatumDogecoinClient {

    private static final Logger log = LoggerFactory.getLogger(TatumDogecoinClient.class);

    private final WebClient webClient;
    private final TatumDogecoinProperties props;

    public TatumDogecoinClient(TatumDogecoinProperties props, WebClient.Builder webClientBuilder) {
        this.props = props;
        this.webClient = webClientBuilder
                .baseUrl(props.getBaseUrl())
                .defaultHeader("X-API-Key",
                props.getApiKey() != null ? props.getApiKey() : "")
            .defaultHeader("x-testnet-type", "DOGE")
                .build();
    }

    public DogeBalanceDto getAddressBalance(String address) {
        ensureApiKey();
        String validationError = DogecoinAddressValidator.validate(address);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }
        try {
            return webClient.get()
                    .uri("/v3/dogecoin/address/balance/{address}", address)
                    .retrieve()
                    .bodyToMono(DogeBalanceDto.class)
                    .block();
        } catch (WebClientResponseException wcre) {
            log.error("Tatum DOGE balance call failed status={} body={}", wcre.getStatusCode(), wcre.getResponseBodyAsString());
            throw wcre;
        } catch (RuntimeException ex) {
            log.error("Failed to call Tatum DOGE /v3/dogecoin/address/balance for {}", address, ex);
            throw ex;
        }
    }

    private void ensureApiKey() {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new IllegalStateException("Tatum DOGE apiKey is not configured (tatum.doge.api-key)");
        }
    }
}
