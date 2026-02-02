package com.ayrotek.dogecoin_pool.etc.service;

import com.ayrotek.dogecoin_pool.etc.client.TatumEtcClient;
import com.ayrotek.dogecoin_pool.etc.config.TatumEtcProperties;
import com.ayrotek.dogecoin_pool.etc.db.EtcSweepTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class EtcSweeperService {

    private static final Logger log = LoggerFactory.getLogger(EtcSweeperService.class);

    // 1 ETC = 10^18 Wei
    private static final BigInteger WEI_PER_ETC = new BigInteger("1000000000000000000");
    
    // Minimum gas reserve (0.02 ETC in Wei)
    private static final BigInteger MIN_GAS_RESERVE_WEI = new BigInteger("20000000000000000");

    private final TatumEtcClient tatumEtcClient;
    private final TatumEtcProperties props;
    private final WebClient webClient;
    private final EtcSweepTransactionRepository sweepTxRepo;

    private final AtomicBoolean sweepInProgress = new AtomicBoolean(false);

    public EtcSweeperService(
            TatumEtcClient tatumEtcClient,
            TatumEtcProperties props,
            EtcSweepTransactionRepository sweepTxRepo,
            WebClient.Builder webClientBuilder
    ) {
        this.tatumEtcClient = tatumEtcClient;
        this.props = props;
        this.sweepTxRepo = sweepTxRepo;
        this.webClient = webClientBuilder
                .baseUrl(props.getBaseUrl())
                .defaultHeader("X-API-Key", Objects.requireNonNullElse(props.getApiKey(), ""))
                .build();
    }

    public EtcSweepResult sweepOnce() {
        if (!sweepInProgress.compareAndSet(false, true)) {
            return persistAndReturn(null, null, 
                new EtcSweepResult(null, null, null, null, false, "Sweep already in progress"));
        }

        try {
            String hot = props.getHotAddress();
            String cold = props.getColdAddress();
            String hotPrivateKey = props.getHotPrivateKey();

            if (hot != null) hot = hot.trim();
            if (cold != null) cold = cold.trim();
            if (hotPrivateKey != null) hotPrivateKey = hotPrivateKey.trim();

            // Validate configuration
            if (hot == null || hot.isBlank()) {
                return persistAndReturn(hot, cold, 
                    new EtcSweepResult(null, null, null, null, false, "HOT address is not configured"));
            }
            if (cold == null || cold.isBlank()) {
                return persistAndReturn(hot, cold, 
                    new EtcSweepResult(null, null, null, null, false, "COLD address is not configured"));
            }
            if (hotPrivateKey == null || hotPrivateKey.isBlank()) {
                return persistAndReturn(hot, cold, 
                    new EtcSweepResult(null, null, null, null, false, "HOT private key is not configured"));
            }

            // Validate addresses
            if (!isValidEtcAddress(hot)) {
                return persistAndReturn(hot, cold, 
                    new EtcSweepResult(null, null, null, null, false, "Invalid HOT address format"));
            }
            if (!isValidEtcAddress(cold)) {
                return persistAndReturn(hot, cold, 
                    new EtcSweepResult(null, null, null, null, false, "Invalid COLD address format"));
            }

            // Get balance
            Map<String, Object> balanceResponse = tatumEtcClient.getAddressBalance(hot);
            String balanceWeiStr = balanceResponse.get("balance").toString();
            BigInteger balanceWei = new BigInteger(balanceWeiStr);
            
            BigDecimal balanceEtc = new BigDecimal(balanceWei).divide(new BigDecimal(WEI_PER_ETC), 18, BigDecimal.ROUND_DOWN);
            
            log.info("[ETC] Hot wallet {} balance: {} Wei ({} ETC)", hot, balanceWei, balanceEtc);

            // Check minimum sweep threshold
            BigDecimal minSweep = BigDecimal.valueOf(props.getMinSweepEtc());
            if (balanceEtc.compareTo(minSweep) < 0) {
                String msg = String.format("Balance %.4f ETC < minSweep %.4f ETC. Skipping.", 
                    balanceEtc, minSweep);
                log.info("[ETC] {}", msg);
                return persistAndReturn(hot, cold, 
                    new EtcSweepResult(balanceWeiStr, null, null, balanceWeiStr, false, msg));
            }

            // Calculate reserve in Wei
            BigDecimal reserveEtc = BigDecimal.valueOf(props.getReserveEtc());
            BigInteger reserveWei = reserveEtc.multiply(new BigDecimal(WEI_PER_ETC)).toBigInteger();
            
            // Ensure minimum gas reserve
            if (reserveWei.compareTo(MIN_GAS_RESERVE_WEI) < 0) {
                log.warn("[ETC] Reserve {} Wei is below minimum {}. Using minimum.", 
                    reserveWei, MIN_GAS_RESERVE_WEI);
                reserveWei = MIN_GAS_RESERVE_WEI;
            }

            // Calculate amount to sweep
            BigInteger amountToSweepWei = balanceWei.subtract(reserveWei);
            
            if (amountToSweepWei.compareTo(BigInteger.ZERO) <= 0) {
                String msg = String.format("After reserve, nothing to sweep. Balance: %s Wei, Reserve: %s Wei", 
                    balanceWei, reserveWei);
                log.info("[ETC] {}", msg);
                return persistAndReturn(hot, cold, 
                    new EtcSweepResult(balanceWeiStr, null, null, balanceWeiStr, false, msg));
            }

            BigDecimal amountToSweepEtc = new BigDecimal(amountToSweepWei).divide(new BigDecimal(WEI_PER_ETC), 18, BigDecimal.ROUND_DOWN);
            log.info("[ETC] Will sweep {} Wei ({} ETC) from {} to {}", 
                amountToSweepWei, amountToSweepEtc, hot, cold);

            TatumEtcClient.TxResult txResult = tatumEtcClient.sendTransaction(hot, hotPrivateKey, cold, amountToSweepWei);
            BigInteger gasUsedWei = txResult.gasPriceWei().multiply(txResult.gasLimit());

            String msg = String.format("Sweep submitted: %s ETC (%s Wei) from %s to %s. txHash=%s", 
                amountToSweepEtc, amountToSweepWei, hot, cold, txResult.txHash());
            log.info("[ETC] {}", msg);
            
            return persistAndReturn(hot, cold, 
                new EtcSweepResult(balanceWeiStr, amountToSweepWei.toString(), gasUsedWei.toString(), balanceWeiStr, true, msg, txResult.txHash()));

        } catch (Exception e) {
            log.error("[ETC] Sweep failed", e);
            return persistAndReturn(props.getHotAddress(), props.getColdAddress(), 
                new EtcSweepResult(null, null, null, null, false, "Exception: " + e.getMessage()));
        } finally {
            sweepInProgress.set(false);
        }
    }

    private boolean isValidEtcAddress(String address) {
        return address != null && address.matches("^0x[a-fA-F0-9]{40}$");
    }

    private EtcSweepResult persistAndReturn(String hot, String cold, EtcSweepResult result) {
        try {
            sweepTxRepo.insert(
                    hot,
                    cold,
                    result.txHash(),
                    result.submitted(),
                    result.hotBalanceWei(),
                    result.amountSentWei(),
                    result.gasUsedWei(),
                    result.updatedHotBalanceWei(),
                    result.message()
            );
        } catch (Exception e) {
            log.error("[ETC] Failed to persist sweep transaction", e);
        }
        return result;
    }

    public record EtcSweepResult(
            String hotBalanceWei,
            String amountSentWei,
            String gasUsedWei,
            String updatedHotBalanceWei,
            boolean submitted,
            String message,
            String txHash
    ) {
        public EtcSweepResult(String hotBalanceWei, String amountSentWei, String gasUsedWei, 
                            String updatedHotBalanceWei, boolean submitted, String message) {
            this(hotBalanceWei, amountSentWei, gasUsedWei, updatedHotBalanceWei, submitted, message, null);
        }
    }
}
