package com.ayrotek.pool_ser.api.mapper;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.web3j.utils.Convert;

import com.ayrotek.pool_ser.api.dto.SweepRecordDto;
import com.ayrotek.pool_ser.entity.SweepRecord;

public final class SweepRecordMapper {

    private SweepRecordMapper() {
    }

    public static SweepRecordDto toDto(SweepRecord entity) {
        if (entity == null) {
            return null;
        }

        return new SweepRecordDto(
                entity.getId(),
                entity.getFromAddress(),
                entity.getToAddress(),
                toPlainString(entity.getAmountWei()),
                toEth(entity.getAmountWei()),
                toPlainString(entity.getGasLimit()),
                toPlainString(entity.getEffectiveFeePerGasWei()),
                toGwei(entity.getEffectiveFeePerGasWei()),
                toPlainString(entity.getGasCostWei()),
                toEth(entity.getGasCostWei()),
                toPlainString(entity.getNonce()),
                toPlainString(entity.getChainId()),
                entity.getTxHash(),
                entity.getStatus() != null ? entity.getStatus().name() : null,
                Integer.valueOf(entity.getRetryCount()),
                entity.getLastError(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getLastCheckedAt());
    }

    private static String toPlainString(BigInteger value) {
        return value != null ? value.toString() : null;
    }

    private static String toEth(BigInteger weiValue) {
        return toUnit(weiValue, Convert.Unit.ETHER);
    }

    private static String toGwei(BigInteger weiValue) {
        return toUnit(weiValue, Convert.Unit.GWEI);
    }

    private static String toUnit(BigInteger weiValue, Convert.Unit unit) {
        if (weiValue == null) {
            return null;
        }
        BigDecimal asDecimal = new BigDecimal(weiValue);
        return Convert.fromWei(asDecimal, unit).stripTrailingZeros().toPlainString();
    }
}
