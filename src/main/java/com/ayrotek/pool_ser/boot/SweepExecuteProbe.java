package com.ayrotek.pool_ser.boot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.web3j.crypto.Credentials;
import org.web3j.utils.Convert;

import com.ayrotek.pool_ser.service.SweepPlanner;
import com.ayrotek.pool_ser.service.SweepPlanner.Plan;
import com.ayrotek.pool_ser.service.SweeperService;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SweepExecuteProbe implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SweepExecuteProbe.class);

    private final Credentials credentials;
    private final SweepPlanner sweepPlanner;
    private final SweeperService sweeperService;

    public SweepExecuteProbe(
            Credentials credentials,
            SweepPlanner sweepPlanner,
            SweeperService sweeperService) {
        this.credentials = credentials;
        this.sweepPlanner = sweepPlanner;
        this.sweeperService = sweeperService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!shouldExecute(System.getenv("SWEEPER_EXECUTE_ON_STARTUP"))) {
            log.info("Sweeper configured but SWEEPER_EXECUTE_ON_STARTUP is not true; no startup transaction sent.");
            return;
        }

        final Plan plan;
        try {
            String from = credentials.getAddress();
            String to = resolveColdAddress(System.getenv("SWEEPER_COLD_ADDRESS"));
            plan = sweepPlanner.plan(from, to);
        } catch (Exception ex) {
            log.error("Failed to plan sweep: {}", ex.getMessage());
            log.debug("Sweep planning failure", ex);
            return;
        }

        log.info(buildSummary(plan));

        if (!plan.sweepable()) {
            log.warn("Not sweepable, nothing sent.");
            return;
        }

        Optional<String> txHash = sweeperService.sweepOnce(plan);
        if (txHash.isPresent()) {
            log.info("Sweep transaction hash: {}", txHash.get());
        } else {
            log.error("Sweep transaction failed; see previous errors.");
        }
    }

    private boolean shouldExecute(String rawFlag) {
        return StringUtils.hasText(rawFlag) && "true".equalsIgnoreCase(rawFlag.trim());
    }

    private String resolveColdAddress(String coldEnv) {
        if (!StringUtils.hasText(coldEnv)) {
            throw new IllegalStateException("SWEEPER_COLD_ADDRESS environment variable is required");
        }
        return coldEnv.trim();
    }

    private String buildSummary(Plan plan) {
        BigDecimal balanceEth = toEth(plan.balanceWei());
        BigDecimal gasCostEth = toEth(plan.gasCostWei());
        BigDecimal sweepEth = toEth(plan.sweepAmountWei());
        BigDecimal priorityGwei = toGwei(plan.priorityFeeWei());
        BigDecimal maxFeeGwei = toGwei(plan.maxFeeWei());

        return new StringBuilder()
                .append("Sweep plan:\n")
                .append("  from: ").append(plan.from()).append(" -> to: ").append(plan.to()).append('\n')
                .append("  chainId: ").append(plan.chainId()).append(" nonce: ").append(plan.nonce()).append('\n')
                .append("  balance: ").append(plan.balanceWei()).append(" Wei (")
                .append(balanceEth).append(" ETH)\n")
                .append("  priorityFee: ").append(priorityGwei).append(" gwei maxFee: ")
                .append(maxFeeGwei).append(" gwei gasLimit: ").append(plan.gasLimit())
                .append(" gasCost: ").append(plan.gasCostWei()).append(" Wei (")
                .append(gasCostEth).append(" ETH)\n")
                .append("  sweepAmount: ").append(plan.sweepAmountWei()).append(" Wei (")
                .append(sweepEth).append(" ETH) sweepable: ")
                .append(plan.sweepable())
                .toString();
    }

    private BigDecimal toEth(java.math.BigInteger wei) {
        return scale(Convert.fromWei(new BigDecimal(wei), Convert.Unit.ETHER));
    }

    private BigDecimal toGwei(java.math.BigInteger wei) {
        return scale(Convert.fromWei(new BigDecimal(wei), Convert.Unit.GWEI));
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(9, RoundingMode.DOWN).stripTrailingZeros();
    }
}