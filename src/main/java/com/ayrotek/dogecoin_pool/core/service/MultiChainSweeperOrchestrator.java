package com.ayrotek.dogecoin_pool.core.service;

import com.ayrotek.dogecoin_pool.core.domain.ChainId;
import com.ayrotek.dogecoin_pool.core.domain.GenericSweepPlan;
import com.ayrotek.dogecoin_pool.core.domain.GenericSweepResult;
import com.ayrotek.dogecoin_pool.core.domain.SweepContext;
import com.ayrotek.dogecoin_pool.core.port.ChainSweeperAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MultiChainSweeperOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(MultiChainSweeperOrchestrator.class);

    private final Map<ChainId, ChainSweeperAdapter> adapters;

    public MultiChainSweeperOrchestrator(List<ChainSweeperAdapter> adapterList) {
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(ChainSweeperAdapter::chainId, a -> a));
    }

    /**
     * High-level sweep entry point:
     * 1) Find the adapter for context.chainId()
     * 2) Ask it to plan a sweep
     * 3) If plan is non-null and amount > 0, execute it
     */
    public GenericSweepResult sweep(SweepContext context) {
        ChainSweeperAdapter adapter = adapters.get(context.chainId());
        if (adapter == null) {
            log.warn("No ChainSweeperAdapter for chainId={}", context.chainId());
            return null;
        }

        log.info("Starting generic sweep: chainId={}, asset={}, hot={}, cold={}, minAmount={}, reserve={}",
                context.chainId(), context.assetSymbol(), context.hotAddress(),
                context.coldAddress(), context.minAmount(), context.reserve());

        GenericSweepPlan plan = adapter.planSweep(context);
        if (plan == null || plan.amount() == null ||
                plan.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Adapter returned no sweep plan (nothing to sweep) for chainId={}", context.chainId());
            return null;
        }

        log.info("Plan created: chainId={}, amount={}, feeEstimate={}, from={}, to={}",
                plan.chainId(), plan.amount(), plan.feeEstimate(), plan.fromAddress(), plan.toAddress());

        GenericSweepResult result = adapter.executeSweep(plan);
        if (result == null) {
            log.warn("Adapter.executeSweep returned null for chainId={}", context.chainId());
            return null;
        }

        if (result.submitted()) {
            log.info("Sweep submitted: txHash={}", result.txHash());
        } else {
            log.warn("Sweep not submitted for chainId={} from={} to={} amount={}",
                    result.chainId(), result.fromAddress(), result.toAddress(), result.amount());
        }

        return result;
    }
}
