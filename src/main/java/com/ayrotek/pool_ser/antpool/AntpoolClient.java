package com.ayrotek.pool_ser.antpool;

import java.time.Duration;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.ayrotek.pool_ser.antpool.dto.AccountBalanceDto;
import com.ayrotek.pool_ser.antpool.dto.AntpoolResponse;
import com.ayrotek.pool_ser.antpool.dto.OverviewDto;

@Service
public class AntpoolClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final String ACCOUNT_BALANCE_PATH = "/account/walletBalance.htm";
    private static final String OVERVIEW_PATH = "/account/overview.htm";

    private static final ParameterizedTypeReference<AntpoolResponse<AccountBalanceDto>> ACCOUNT_BALANCE_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<AntpoolResponse<OverviewDto>> OVERVIEW_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final WebClient webClient;
    private final AntpoolProperties properties;

    public AntpoolClient(WebClient antpoolWebClient, AntpoolProperties properties) {
        this.webClient = antpoolWebClient;
        this.properties = properties;
    }

    public AntpoolResponse<AccountBalanceDto> getAccountBalance(String coin) {
        MultiValueMap<String, String> params = buildAuthParams();
        params.add("coin", resolveCoin(coin));
        AntpoolResponse<AccountBalanceDto> response = executeGet(ACCOUNT_BALANCE_PATH, params, ACCOUNT_BALANCE_TYPE);
        ensureSuccess(response, "account balance");
        return response;
    }

    public AntpoolResponse<OverviewDto> getOverview(String coin) {
        MultiValueMap<String, String> params = buildAuthParams();
        params.add("coin", resolveCoin(coin));
        params.add("userId", properties.getUserId());
        AntpoolResponse<OverviewDto> response = executeGet(OVERVIEW_PATH, params, OVERVIEW_TYPE);
        ensureSuccess(response, "account overview");
        return response;
    }

    private MultiValueMap<String, String> buildAuthParams() {
        properties.assertCredentialsPresent();
        String nonce = String.valueOf(System.currentTimeMillis());
        String sign = AntpoolSigner.sign(properties.getApiKey(), properties.getApiSecret(), nonce);
        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("key", properties.getApiKey());
        params.add("nonce", nonce);
        params.add("sign", sign);
        return params;
    }

    private <T> AntpoolResponse<T> executeGet(String path,
                                              MultiValueMap<String, String> params,
                                              ParameterizedTypeReference<AntpoolResponse<T>> typeReference) {
        try {
            return webClient.get()
                    .uri(builder -> builder.path(path).queryParams(params).build())
                    .retrieve()
                    .bodyToMono(typeReference)
                    .block(REQUEST_TIMEOUT);
        } catch (WebClientResponseException | WebClientRequestException ex) {
            throw new IllegalStateException("AntPool request failed for " + path + ": " + ex.getMessage(), ex);
        }
    }

    private <T> void ensureSuccess(AntpoolResponse<T> response, String operation) {
        if (response == null) {
            throw new IllegalStateException("AntPool returned empty response for " + operation);
        }
        if (response.code() != 0) {
            throw new IllegalStateException("AntPool " + operation + " failed: " + response.message());
        }
    }

    private String resolveCoin(String coin) {
        return (coin == null || coin.isBlank()) ? properties.getDefaultCoin() : coin;
    }
}
