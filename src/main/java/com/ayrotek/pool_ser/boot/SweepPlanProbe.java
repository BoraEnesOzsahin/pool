package com.ayrotek.pool_ser.boot;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.web3j.crypto.Credentials;
import org.web3j.utils.Convert;

import com.ayrotek.pool_ser.service.SweepPlanner;
import com.ayrotek.pool_ser.service.SweepPlanner.Plan;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class SweepPlanProbe implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SweepPlanProbe.class);

    private final Credentials credentials;
    private final SweepPlanner sweepPlanner;
    private final Environment environment;
    private final ConfigurableApplicationContext context;

    public SweepPlanProbe(
            Credentials credentials,
            SweepPlanner sweepPlanner,
            Environment environment,
            ConfigurableApplicationContext context) {
        this.credentials = credentials;
        this.sweepPlanner = sweepPlanner;
        this.environment = environment;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        final String hotAddress = credentials.getAddress();
        final String coldAddress = resolveColdAddress();

        try {
            Plan plan = sweepPlanner.plan(hotAddress, coldAddress);
            log.info(buildSummary(plan));
            exit(0);
        } catch (Exception ex) {
            log.error("Failed to plan sweep: {}", ex.getMessage(), ex);
            exit(1);
        }
    }

    private String resolveColdAddress() {
        String cold = environment.getProperty("SWEEPER_COLD_ADDRESS");
        if (!StringUtils.hasText(cold)) {
            throw new IllegalStateException("SWEEPER_COLD_ADDRESS environment variable is required");
        }
        return cold.trim();
    }

    private void exit(int status) {
        int code = org.springframework.boot.SpringApplication.exit(context, () -> status);
        System.exit(code);
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

    private BigDecimal toEth(BigDecimal wei) {
        return scale(Convert.fromWei(wei, Convert.Unit.ETHER));
    }

    private BigDecimal toEth(java.math.BigInteger wei) {
        return toEth(new BigDecimal(wei));
    }

    private BigDecimal toGwei(java.math.BigInteger wei) {
        return scale(Convert.fromWei(new BigDecimal(wei), Convert.Unit.GWEI));
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(9, RoundingMode.DOWN).stripTrailingZeros();
    }
}
