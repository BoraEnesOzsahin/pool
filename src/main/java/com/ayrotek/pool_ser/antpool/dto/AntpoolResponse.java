package com.ayrotek.pool_ser.antpool.dto;

public record AntpoolResponse<T>(
        int code,
        String message,
        T data
) {
}
