package com.ayrotek.dogecoin_pool.core.domain;

import java.math.BigDecimal;

/**
 * Chain-agnostic sweep plan produced by an adapter.
 * internalRef is opaque metadata used by the adapter (nonce, UTXO info, etc.).
 */
public record GenericSweepPlan(
        ChainId chainId,
        String assetSymbol,
        String fromAddress,
        String toAddress,
        BigDecimal amount,
        BigDecimal feeEstimate,
        String internalRef
) {
}
