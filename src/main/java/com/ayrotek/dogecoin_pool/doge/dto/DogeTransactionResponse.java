package com.ayrotek.dogecoin_pool.doge.dto;

/**
 * Simplified transaction response; adjust field names to match Tatum JSON exactly.
 */
public record DogeTransactionResponse(
        String txId,
        String failed,
        String rawTx
) {
}
