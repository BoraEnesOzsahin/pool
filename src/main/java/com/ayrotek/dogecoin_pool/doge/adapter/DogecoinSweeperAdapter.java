package com.ayrotek.dogecoin_pool.doge.adapter;

import com.ayrotek.dogecoin_pool.core.domain.ChainId;
import com.ayrotek.dogecoin_pool.core.domain.GenericSweepPlan;
import com.ayrotek.dogecoin_pool.core.domain.GenericSweepResult;
import com.ayrotek.dogecoin_pool.core.domain.SweepContext;
import com.ayrotek.dogecoin_pool.core.port.ChainSweeperAdapter;
import com.ayrotek.dogecoin_pool.doge.client.TatumDogecoinClient;
import com.ayrotek.dogecoin_pool.doge.config.TatumDogecoinProperties;
import com.ayrotek.dogecoin_pool.doge.dto.DogeBalanceDto;
import com.ayrotek.dogecoin_pool.doge.dto.DogeFromAddress;
import com.ayrotek.dogecoin_pool.doge.dto.DogeToAddress;
import com.ayrotek.dogecoin_pool.doge.dto.DogeTransactionAddressRequest;
import com.ayrotek.dogecoin_pool.doge.dto.DogeTransactionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class DogecoinSweeperAdapter implements ChainSweeperAdapter {

    private static final Logger log = LoggerFactory.getLogger(DogecoinSweeperAdapter.class);

    // Dogecoin uses 8 decimal places. Tatum/DOGE will reject dust change outputs.
    // Use a conservative minimum change (reserve) to avoid dust.
    private static final BigDecimal MIN_CHANGE_DOGE = new BigDecimal("1.0");
    private static final BigDecimal FIXED_FEE_DOGE = new BigDecimal("1.0");

    private final TatumDogecoinClient tatumClient;
    private final TatumDogecoinProperties props;
    private final WebClient webClient;

    public DogecoinSweeperAdapter(
            TatumDogecoinClient tatumClient,
            TatumDogecoinProperties props,
            WebClient.Builder webClientBuilder
    ) {
        this.tatumClient = tatumClient;
        this.props = props;
        this.webClient = webClientBuilder
                .baseUrl(props.getBaseUrl())
                .defaultHeader("X-API-Key",
                Objects.requireNonNullElse(props.getApiKey(), ""))
            .defaultHeader("x-testnet-type", "DOGE")
                .build();
    }

    @Override
    public ChainId chainId() {
        return ChainId.DOGE_TESTNET;
    }

    @Override
    public GenericSweepPlan planSweep(SweepContext context) {
        String hot = choose(context.hotAddress(), props.getHotAddress());
        String cold = choose(context.coldAddress(), props.getColdAddress());

        if (hot == null || hot.isBlank()) {
            log.warn("[DOGE_TESTNET] HOT address is not configured.");
            return null;
        }
        if (cold == null || cold.isBlank()) {
            log.warn("[DOGE_TESTNET] COLD address is not configured.");
            return null;
        }

        BigDecimal minAmount =
                (context.minAmount() != null && context.minAmount().compareTo(BigDecimal.ZERO) > 0)
                        ? context.minAmount()
                        : BigDecimal.valueOf(props.getMinSweepDoge());

        BigDecimal reserve =
                (context.reserve() != null && context.reserve().compareTo(BigDecimal.ZERO) >= 0)
                        ? context.reserve()
                        : BigDecimal.valueOf(props.getReserveDoge());

        if (reserve.compareTo(MIN_CHANGE_DOGE) < 0) {
            log.warn("[DOGE_TESTNET] Reserve {} is below dust-safe minimum {}. Bumping reserve to {} DOGE.",
                reserve, MIN_CHANGE_DOGE, MIN_CHANGE_DOGE);
            reserve = MIN_CHANGE_DOGE;
        }

        DogeBalanceDto balanceDto = tatumClient.getAddressBalance(hot);
        BigDecimal balance = parseBalance(balanceDto);

        log.info("[DOGE_TESTNET] HOT balance={} DOGE (minSweep={}, reserve={})",
                balance, minAmount, reserve);

        if (balance.compareTo(minAmount) <= 0) {
            log.info("[DOGE_TESTNET] Balance {} <= minSweep {}, nothing to sweep.", balance, minAmount);
            return null;
        }

        BigDecimal sweepAmount = balance.subtract(reserve);
        if (sweepAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[DOGE_TESTNET] Sweep amount {} <= 0 after reserve, skipping.", sweepAmount);
            return null;
        }

        sweepAmount = sweepAmount.setScale(8, RoundingMode.DOWN);

        BigDecimal feeEstimate = BigDecimal.ZERO;
        String internalRef = "from=" + hot + ";to=" + cold;

        String asset = context.assetSymbol() != null ? context.assetSymbol() : "DOGE";

        return new GenericSweepPlan(
                ChainId.DOGE_TESTNET,
                asset,
                hot,
                cold,
                sweepAmount,
                feeEstimate,
                internalRef
        );
    }

    @Override
    public GenericSweepResult executeSweep(GenericSweepPlan plan) {
        if (plan == null || plan.amount() == null ||
                plan.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.info("[DOGE_TESTNET] executeSweep called with null or zero amount, skipping.");
            return plan == null
                    ? null
                    : new GenericSweepResult(plan.chainId(), plan.assetSymbol(),
                    plan.fromAddress(), plan.toAddress(),
                    plan.amount(), null, false);
        }

        if (props.getHotPrivateKey() == null || props.getHotPrivateKey().isBlank()) {
            log.error("[DOGE_TESTNET] Hot private key is not configured, cannot sweep.");
            return new GenericSweepResult(plan.chainId(), plan.assetSymbol(),
                    plan.fromAddress(), plan.toAddress(),
                    plan.amount(), null, false);
        }

        String hot = plan.fromAddress();
        String cold = plan.toAddress();
        BigDecimal amount = plan.amount();

        BigDecimal fee = FIXED_FEE_DOGE;
        BigDecimal sendAmount = amount.subtract(fee).setScale(8, RoundingMode.DOWN);
        if (sendAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("[DOGE_TESTNET] Sweep amount minus fee <= 0; amount={} fee={}", amount, fee);
            return new GenericSweepResult(plan.chainId(), plan.assetSymbol(), hot, cold, amount, null, false);
        }

        DogeFromAddress from = new DogeFromAddress(hot, props.getHotPrivateKey());
        DogeToAddress to = new DogeToAddress(cold, sendAmount);

        DogeTransactionAddressRequest body = new DogeTransactionAddressRequest(
            List.of(from),
            List.of(to),
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

            if (resp == null) {
                log.error("[DOGE_TESTNET] Null response from Tatum /v3/dogecoin/transaction.");
                return new GenericSweepResult(plan.chainId(), plan.assetSymbol(),
                        hot, cold, amount, null, false);
            }

            String txId = resp.txId();
            log.info("[DOGE_TESTNET] Sweep broadcasted. txId={}", txId);

            return new GenericSweepResult(plan.chainId(), plan.assetSymbol(),
                    hot, cold, amount, txId, txId != null);

        } catch (Exception ex) {
            if (ex instanceof WebClientResponseException wcre) {
                log.error("[DOGE_TESTNET] Sweep failed status={} headers={} body={}", wcre.getStatusCode(), wcre.getHeaders(), wcre.getResponseBodyAsString());
            } else {
                log.error("[DOGE_TESTNET] Failed to execute Dogecoin sweep via Tatum.", ex);
            }
            return new GenericSweepResult(plan.chainId(), plan.assetSymbol(),
                    hot, cold, amount, null, false);
        }
    }

    private String choose(String primary, String fallback) {
        return (primary != null && !primary.isBlank()) ? primary : fallback;
    }

    private BigDecimal parseBalance(DogeBalanceDto dto) {
        BigDecimal direct = parseDoge(dto.balance());
        if (direct.compareTo(BigDecimal.ZERO) > 0) {
            return direct;
        }

        BigDecimal incoming = parseDoge(dto.incoming());
        BigDecimal outgoing = parseDoge(dto.outgoing());
        BigDecimal outgoingPending = parseDoge(dto.outgoingPending());

        // Treat pending outgoing as unavailable; ignore incomingPending until confirmed.
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
}
