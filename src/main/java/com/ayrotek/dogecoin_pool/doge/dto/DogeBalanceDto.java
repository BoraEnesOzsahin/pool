package com.ayrotek.dogecoin_pool.doge.dto;

/**
 * Dogecoin balance response from Tatum.
 * Some responses may omit "balance" and only include incoming/outgoing/pending fields.
 */
public record DogeBalanceDto(
        String incoming,
        String outgoing,
        String incomingPending,
        String outgoingPending,
        String balance
) {
}
