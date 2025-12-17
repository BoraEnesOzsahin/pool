package com.ayrotek.dogecoin_pool.core.domain;

import java.math.BigDecimal;

/**
 * Generic context for a sweep operation, independent of chain.
 */
public record SweepContext(
        ChainId chainId,
        String assetSymbol,
        String hotAddress,
        String coldAddress,
        BigDecimal minAmount,
        BigDecimal reserve,
        BigDecimal maxFee
) {
}
