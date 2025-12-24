package com.ayrotek.dogecoin_pool.doge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * electrs-compatible response for GET /address/{address}.
 */
public record ElectrsAddressResponse(
        String address,
        @JsonProperty("chain_stats") Stats chainStats,
        @JsonProperty("mempool_stats") Stats mempoolStats
) {
    public record Stats(
            @JsonProperty("funded_txo_count") long fundedTxoCount,
            @JsonProperty("funded_txo_sum") long fundedTxoSum,
            @JsonProperty("spent_txo_count") long spentTxoCount,
            @JsonProperty("spent_txo_sum") long spentTxoSum,
            @JsonProperty("tx_count") long txCount
    ) {
    }
}
