package com.ayrotek.dogecoin_pool.doge.service;

import com.ayrotek.dogecoin_pool.doge.client.TatumDogecoinClient;
import com.ayrotek.dogecoin_pool.doge.config.TatumDogecoinProperties;
import com.ayrotek.dogecoin_pool.doge.dto.DogeBalanceDto;
import com.ayrotek.dogecoin_pool.doge.dto.DogeFromAddress;
import com.ayrotek.dogecoin_pool.doge.dto.DogeToAddress;
import com.ayrotek.dogecoin_pool.doge.dto.DogeTransactionAddressRequest;
import com.ayrotek.dogecoin_pool.doge.dto.DogeTransactionResponse;
import com.ayrotek.dogecoin_pool.doge.util.DogecoinAddressValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class DogecoinSweeperService {

    private static final Logger log = LoggerFactory.getLogger(DogecoinSweeperService.class);

    private static final BigDecimal MIN_CHANGE_DOGE = new BigDecimal("1.0");
    private static final BigDecimal FIXED_FEE_DOGE = new BigDecimal("1.0");

    private final TatumDogecoinClient tatumClient;
    private final TatumDogecoinProperties props;
    private final WebClient webClient;

    public DogecoinSweeperService(
            TatumDogecoinClient tatumClient,
            TatumDogecoinProperties props,
            WebClient.Builder webClientBuilder
    ) {
        this.tatumClient = tatumClient;
        this.props = props;
        this.webClient = webClientBuilder
                .baseUrl(props.getBaseUrl())
                .defaultHeader("X-API-Key", Objects.requireNonNullElse(props.getApiKey(), ""))
                .defaultHeader("x-testnet-type", "DOGE")
                .build();
    }

    public DogeSweepResult sweepOnce() {
        String hot = props.getHotAddress();
        String cold = props.getColdAddress();

        if (hot == null || hot.isBlank()) {
            return new DogeSweepResult(null, null, null, false, "HOT address is not configured");
        }
        if (cold == null || cold.isBlank()) {
            return new DogeSweepResult(null, null, null, false, "COLD address is not configured");
        }
        if (props.getHotPrivateKey() == null || props.getHotPrivateKey().isBlank()) {
            return new DogeSweepResult(null, null, null, false, "HOT private key is not configured");
        }

        String hotValidationError = DogecoinAddressValidator.validate(hot);
        if (hotValidationError != null) {
            return new DogeSweepResult(null, null, null, false, "Invalid HOT address: " + hotValidationError);
        }
        String coldValidationError = DogecoinAddressValidator.validate(cold);
        if (coldValidationError != null) {
            return new DogeSweepResult(null, null, null, false, "Invalid COLD address: " + coldValidationError);
        }

        BigDecimal minSweep = BigDecimal.valueOf(props.getMinSweepDoge());
        BigDecimal reserve = BigDecimal.valueOf(props.getReserveDoge());
        if (reserve.compareTo(MIN_CHANGE_DOGE) < 0) {
            log.warn("[DOGE] Reserve {} is below dust-safe minimum {}. Bumping reserve to {} DOGE.",
                    reserve, MIN_CHANGE_DOGE, MIN_CHANGE_DOGE);
            reserve = MIN_CHANGE_DOGE;
        }

        DogeBalanceDto balanceDto;
        try {
            balanceDto = tatumClient.getAddressBalance(hot);
        } catch (WebClientResponseException wcre) {
            String body = wcre.getResponseBodyAsString();
            String msg = "Tatum balance error: " + wcre.getStatusCode();
            if (body != null && !body.isBlank()) {
                msg = msg + " body=" + shorten(body, 300);
            }
            return new DogeSweepResult(null, null, null, false, msg);
        } catch (RuntimeException ex) {
            return new DogeSweepResult(null, null, null, false, "Failed to fetch balance: " + ex.getMessage());
        }

        if (balanceDto == null) {
            return new DogeSweepResult(null, null, null, false, "Tatum returned null balance payload");
        }
        BigDecimal balance = parseBalance(balanceDto);

        log.info("[DOGE] HOT balance={} DOGE (minSweep={}, reserve={})", balance, minSweep, reserve);

        if (balance.compareTo(minSweep) <= 0) {
            return new DogeSweepResult(balance, BigDecimal.ZERO, null, false,
                    "Balance <= min sweep; nothing to do");
        }

        BigDecimal sweepable = balance.subtract(reserve).setScale(8, RoundingMode.DOWN);
        if (sweepable.compareTo(BigDecimal.ZERO) <= 0) {
            return new DogeSweepResult(balance, BigDecimal.ZERO, null, false,
                    "Sweepable amount <= 0 after reserve");
        }

        BigDecimal fee = FIXED_FEE_DOGE;
        BigDecimal sendAmount = sweepable.subtract(fee).setScale(8, RoundingMode.DOWN);
        if (sendAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new DogeSweepResult(balance, BigDecimal.ZERO, null, false,
                    "Sweepable amount minus fee <= 0");
        }

        DogeTransactionAddressRequest body = new DogeTransactionAddressRequest(
                List.of(new DogeFromAddress(hot, props.getHotPrivateKey())),
                List.of(new DogeToAddress(cold, sendAmount)),
                formatDoge(fee),
                hot
        );

        try {
            DogeTransactionResponse resp = webClient.post()
                    .uri("/v3/dogecoin/transaction")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(DogeTransactionResponse.class)
                    .block();

            if (resp == null || resp.txId() == null || resp.txId().isBlank()) {
                return new DogeSweepResult(balance, sendAmount, null, false,
                        "Tatum returned null/empty txId");
            }

            log.info("[DOGE] Sweep broadcasted. txId={}", resp.txId());
            return new DogeSweepResult(balance, sendAmount, resp.txId(), true, "submitted");

        } catch (WebClientResponseException wcre) {
            log.error("[DOGE] Sweep failed status={} body={}", wcre.getStatusCode(), wcre.getResponseBodyAsString());
            return new DogeSweepResult(balance, sendAmount, null, false, "Tatum error: " + wcre.getStatusCode());
        } catch (Exception ex) {
            log.error("[DOGE] Sweep failed", ex);
            return new DogeSweepResult(balance, sendAmount, null, false, "Unexpected error");
        }
    }

    private String shorten(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLen) {
            return trimmed;
        }
        return trimmed.substring(0, maxLen) + "…";
    }

    private BigDecimal parseBalance(DogeBalanceDto dto) {
        BigDecimal direct = parseDoge(dto.balance());
        if (direct.compareTo(BigDecimal.ZERO) > 0) {
            return direct;
        }

        BigDecimal incoming = parseDoge(dto.incoming());
        BigDecimal outgoing = parseDoge(dto.outgoing());
        BigDecimal outgoingPending = parseDoge(dto.outgoingPending());

        BigDecimal computed = incoming.subtract(outgoing).subtract(outgoingPending);
        if (computed.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return computed;
    }

    private BigDecimal parseDoge(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value);
    }

    private String formatDoge(BigDecimal value) {
        return String.format(Locale.ROOT, "%.8f", value);
    }

    public record DogeSweepResult(
            BigDecimal hotBalance,
            BigDecimal amountSent,
            String txId,
            boolean submitted,
            String message
    ) {
    }
}
