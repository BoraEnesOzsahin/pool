package com.ayrotek.pool_ser.job;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

import com.ayrotek.pool_ser.service.SweepPlanner;
import com.ayrotek.pool_ser.service.SweepPlanner.Plan;
import com.ayrotek.pool_ser.service.SweeperService;

@Component
public class SweeperJob {

    private static final Logger log = LoggerFactory.getLogger(SweeperJob.class);
    private static final int DISPLAY_SCALE = 9;

    private final Web3j web3j;
    private final Credentials credentials;
    private final SweepPlanner sweepPlanner;
    private final SweeperService sweeperService;
    private final Environment environment;
    private final BigDecimal sweeperMinSweepEth;
    private final BigDecimal sweeperMaxGasGwei;

    public SweeperJob(
            Web3j web3j,
            Credentials credentials,
            SweepPlanner sweepPlanner,
            SweeperService sweeperService,
            Environment environment,
            @Value("${SWEEPER_MIN_SWEEP_ETH:0.01}") BigDecimal sweeperMinSweepEth,
            @Value("${SWEEPER_MAX_GAS_GWEI:100}") BigDecimal sweeperMaxGasGwei) {
        this.web3j = web3j;
        this.credentials = credentials;
        this.sweepPlanner = sweepPlanner;
        this.sweeperService = sweeperService;
        this.environment = environment;
        this.sweeperMinSweepEth = sweeperMinSweepEth;
        this.sweeperMaxGasGwei = sweeperMaxGasGwei;
    }

    // Periodically evaluate sweep readiness and send at most one sweep transaction.
    @Scheduled(fixedDelayString = "${SWEEPER_POLL_MILLIS:300000}")
    public void run() {
        try {
            if (!isEnabled()) {
                log.info("Sweeper disabled (SWEEPER_ENABLED != true), skipping.");
                return;
            }

            String fromAddress = credentials != null ? credentials.getAddress() : null;
            if (!StringUtils.hasText(fromAddress)) {
                log.error("Hot wallet credentials missing address; skipping sweep job.");
                return;
            }
            fromAddress = fromAddress.trim();

            String coldAddress = resolveColdAddress();
            if (!StringUtils.hasText(coldAddress)) {
                log.error("SWEEPER_COLD_ADDRESS not configured; cannot execute sweeps.");
                return;
            }

            Plan plan = planSweep(fromAddress, coldAddress);
            if (plan == null) {
                return;
            }

            if (!plan.sweepable()) {
                log.info("Sweep skipped: plan not sweepable for {} (balance {} wei).", fromAddress, plan.balanceWei());
                return;
            }

            BigDecimal sweepAmountEth = toDecimal(plan.sweepAmountWei(), Unit.ETHER);
            BigDecimal effectiveGasGwei = toDecimal(plan.effectiveFeePerGasWei(), Unit.GWEI);

            if (sweepAmountEth.compareTo(sweeperMinSweepEth) < 0) {
                log.info("Sweep skipped: {} ETH below minimum threshold {} ETH.", format(sweepAmountEth), format(sweeperMinSweepEth));
                return;
            }

            if (effectiveGasGwei.compareTo(sweeperMaxGasGwei) > 0) {
                log.info("Sweep skipped: gas {} gwei exceeds cap {} gwei.", format(effectiveGasGwei), format(sweeperMaxGasGwei));
                return;
            }

            log.info(
                    "Attempting sweep: from {} to {} amount {} ETH ({} wei) gas {} gwei limit {}",
                    fromAddress,
                    plan.to(),
                    format(sweepAmountEth),
                    plan.sweepAmountWei(),
                    format(effectiveGasGwei),
                    plan.gasLimit());

            Optional<String> txHash = sweeperService.sweepOnce(plan);
            if (txHash.isPresent()) {
                log.info("Sweep succeeded with txHash {}", txHash.get());
            } else {
                log.warn("Sweep attempt completed without transaction hash.");
            }
        } catch (Exception ex) {
            log.error("Unexpected sweeper job failure: {}", ex.getMessage());
            log.debug("Unexpected sweeper job failure", ex);
        }
    }

    private Plan planSweep(String fromAddress, String coldAddress) {
        try {
            Plan plan = sweepPlanner.plan(fromAddress, coldAddress);
            if (log.isDebugEnabled()) {
                BigDecimal balanceEth = toDecimal(plan.balanceWei(), Unit.ETHER);
                log.debug("Hot wallet {} balance {} ETH", fromAddress, format(balanceEth));
            }
            return plan;
        } catch (Exception ex) {
            log.error("Failed to plan sweep from {} to {}: {}", fromAddress, coldAddress, ex.getMessage());
            log.debug("Sweep planning failure", ex);
            return null;
        }
    }

    private boolean isEnabled() {
        String value = environment.getProperty("SWEEPER_ENABLED", "false");
        return value != null && "true".equalsIgnoreCase(value.trim());
    }

    private String resolveColdAddress() {
        String coldAddress = environment.getProperty("SWEEPER_COLD_ADDRESS");
        if (!StringUtils.hasText(coldAddress)) {
            coldAddress = System.getenv("SWEEPER_COLD_ADDRESS");
        }
        return coldAddress != null ? coldAddress.trim() : null;
    }

    private BigDecimal toDecimal(BigInteger valueWei, Unit unit) {
        return scale(Convert.fromWei(new BigDecimal(valueWei), unit));
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(DISPLAY_SCALE, RoundingMode.DOWN).stripTrailingZeros();
    }

    private String format(BigDecimal value) {
        return value.toPlainString();
    }
}
