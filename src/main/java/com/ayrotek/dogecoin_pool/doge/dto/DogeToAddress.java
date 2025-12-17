package com.ayrotek.dogecoin_pool.doge.dto;

import java.math.BigDecimal;

public record DogeToAddress(
        String address,
        BigDecimal value
) {
}
