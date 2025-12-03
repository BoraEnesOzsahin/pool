package com.ayrotek.pool_ser.api.dto;

import java.time.Instant;

public record SweepRecordDto(
        Long id,
        String fromAddress,
        String toAddress,
        String amountWei,
        String amountEth,
        String gasLimit,
        String effectiveFeePerGasWei,
        String effectiveFeePerGasGwei,
        String gasCostWei,
        String gasCostEth,
        String nonce,
        String chainId,
        String txHash,
        String status,
        Integer retryCount,
        String lastError,
        Instant createdAt,
        Instant updatedAt,
        Instant lastCheckedAt
) {
}
