package com.ayrotek.pool_ser.api.dto;

public record Web3HealthDto(
        boolean ok,
        String clientVersion,
        String networkId,
        String error
) {
}
