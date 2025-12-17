package com.ayrotek.dogecoin_pool.core.domain;

import java.math.BigDecimal;

/**
 * Generic result after executing a sweep.
 */
public record GenericSweepResult(
        ChainId chainId,
        String assetSymbol,
        String fromAddress,
        String toAddress,
        BigDecimal amount,
        String txHash,
        boolean submitted
) {
}
