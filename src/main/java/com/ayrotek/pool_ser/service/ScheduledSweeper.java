package com.ayrotek.pool_ser.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.web3j.crypto.Credentials;
import org.web3j.utils.Convert;

import com.ayrotek.pool_ser.config.SweeperProperties;
import com.ayrotek.pool_ser.service.SweepPlanner.Plan;

@Service
public class ScheduledSweeper {

    private static final Logger log = LoggerFactory.getLogger(ScheduledSweeper.class);

    private final Credentials credentials;
    private final SweepPlanner sweepPlanner;
    private final SweeperService sweeperService;
    private final SweeperProperties properties;

    public ScheduledSweeper(
            Credentials credentials,
            SweepPlanner sweepPlanner,
            SweeperService sweeperService,
            SweeperProperties properties) {
        this.credentials = credentials;
        this.sweepPlanner = sweepPlanner;
        this.sweeperService = sweeperService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "#{@sweeperProperties.pollDelayString()}")
    public void sweepLoop() {
        if (!properties.enabled()) {
            log.info("Scheduled sweeper disabled; set SWEEPER_ENABLED=true to activate.");
            return;
        }

        final String coldAddress = properties.coldAddress();
        if (!StringUtils.hasText(coldAddress)) {
            log.error("SWEEPER_COLD_ADDRESS is not configured; skipping sweep attempt.");
            return;
        }

        final String fromAddress = resolveHotAddress();
        if (!StringUtils.hasText(fromAddress)) {
            log.error("Hot wallet credentials do not expose an address; skipping sweep attempt.");
            return;
        }

        final Plan plan;
        try {
            plan = sweepPlanner.plan(fromAddress, coldAddress);
        } catch (Exception ex) {
            log.error("Planning sweep failed: {}", ex.getMessage());
            log.debug("Planning failure", ex);
            return;
        }

        BigDecimal sweepAmountEth = toEth(plan.sweepAmountWei());
        BigDecimal minSweepEth = properties.minSweepEth();
        if (!plan.sweepable() || sweepAmountEth.compareTo(minSweepEth) < 0) {
            log.info("Sweep not sent; sweepable={} amount={} ETH threshold={} ETH.",
                    plan.sweepable(),
                    sweepAmountEth,
                    minSweepEth);
            return;
        }

        BigDecimal balanceEth = toEth(plan.balanceWei());

        log.info(
            "Sweeping {} ETH ({} Wei) from {} to {} | balance {} ETH ({} Wei) | gasLimit {} maxFee {} gwei priorityFee {} gwei nonce {}",
            sweepAmountEth,
            plan.sweepAmountWei(),
            plan.from(),
            plan.to(),
            balanceEth,
            plan.balanceWei(),
            plan.gasLimit(),
            toGwei(plan.maxFeeWei()),
            toGwei(plan.priorityFeeWei()),
            plan.nonce());

        try {
            Optional<String> txHash = sweeperService.sweepOnce(plan);
            if (txHash.isPresent()) {
                log.info("Sweep tx sent: {}", txHash.get());
            } else {
                log.warn("Sweep attempt failed or aborted; no tx hash.");
            }
        } catch (Exception ex) {
            log.error("Unexpected error during sweep execution: {}", ex.getMessage());
            log.debug("Sweep execution failure", ex);
        }
    }

    private String resolveHotAddress() {
        if (credentials == null) {
            return null;
        }
        return credentials.getAddress();
    }

    private BigDecimal toEth(BigInteger wei) {
        if (wei == null) {
            return BigDecimal.ZERO;
        }
        return scale(Convert.fromWei(new BigDecimal(wei), Convert.Unit.ETHER));
    }

    private BigDecimal toGwei(BigInteger wei) {
        if (wei == null) {
            return BigDecimal.ZERO;
        }
        return scale(Convert.fromWei(new BigDecimal(wei), Convert.Unit.GWEI));
    }

    private BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(9, RoundingMode.DOWN).stripTrailingZeros();
    }
}
