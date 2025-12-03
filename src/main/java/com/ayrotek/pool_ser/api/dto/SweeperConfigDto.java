package com.ayrotek.pool_ser.api.dto;

public record SweeperConfigDto(
        String hotAddress,
        String coldAddress,
        boolean sweeperEnabled,
        Long sweeperPollMillis,
        Double minSweepEth,
        Double maxGasGwei,
        boolean receiptPollEnabled,
        Long receiptPollMillis,
        Integer receiptMaxRetries
) {
}
