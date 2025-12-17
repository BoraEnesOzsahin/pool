package com.ayrotek.dogecoin_pool.core.port;

import com.ayrotek.dogecoin_pool.core.domain.ChainId;
import com.ayrotek.dogecoin_pool.core.domain.GenericSweepPlan;
import com.ayrotek.dogecoin_pool.core.domain.GenericSweepResult;
import com.ayrotek.dogecoin_pool.core.domain.SweepContext;

/**
 * Chain-specific sweeper adapter.
 * Each blockchain/asset implements this interface to:
 *  - plan a sweep from HOT -> COLD
 *  - execute a previously planned sweep
 */
public interface ChainSweeperAdapter {

    /**
     * @return the chain this adapter handles, e.g. DOGE_TESTNET.
     */
    ChainId chainId();

    /**
     * Plan a sweep for the given context.
     * If there's nothing to sweep (e.g. balance below threshold),
     * this method may return null.
     */
    GenericSweepPlan planSweep(SweepContext context);

    /**
     * Execute a previously planned sweep.
     * Plan must have been created by this adapter.
     */
    GenericSweepResult executeSweep(GenericSweepPlan plan);
}
