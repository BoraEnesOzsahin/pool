package com.ayrotek.pool_ser.antpool.dto;

public record OverviewDto(
        String coin,
        String userId,
        String totalAmount,
        String unpaidAmount,
        String yesterdayAmount,
        long totalWorkerNum,
        long activeWorkerNum,
        long inactiveWorkerNum,
        long invalidWorkerNum
) {
}
