package com.ayrotek.dogecoin_pool.doge.dto;

import java.util.List;

/**
 * Request body for POST /v3/dogecoin/transaction using fromAddress mode.
 * Adjust fields if Tatum's DOGE docs require additional parameters.
 */
public record DogeTransactionAddressRequest(
        List<DogeFromAddress> fromAddress,
        List<DogeToAddress> to,
        String fee,
        String changeAddress
) {
}
