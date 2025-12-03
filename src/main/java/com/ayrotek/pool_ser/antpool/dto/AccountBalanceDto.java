package com.ayrotek.pool_ser.antpool.dto;

public record AccountBalanceDto(
        String coin,
        String availableAmount,
        String unpaidAmount,
        String totalAmount
) {
}
