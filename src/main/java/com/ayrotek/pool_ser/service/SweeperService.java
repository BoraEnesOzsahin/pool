package com.ayrotek.pool_ser.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import com.ayrotek.pool_ser.entity.SweepRecord;
import com.ayrotek.pool_ser.entity.SweepStatus;
import com.ayrotek.pool_ser.repository.SweepRecordRepository;
import com.ayrotek.pool_ser.service.SweepPlanner.Plan;

@Service
public class SweeperService {

    private static final Logger log = LoggerFactory.getLogger(SweeperService.class);

    private final Web3j web3j;
    private final Credentials credentials;
    private final SweepRecordRepository sweepRecordRepository;

    public SweeperService(Web3j web3j, Credentials credentials, SweepRecordRepository sweepRecordRepository) {
        this.web3j = web3j;
        this.credentials = credentials;
        this.sweepRecordRepository = sweepRecordRepository;
    }

    public Optional<String> sweepOnce(Plan plan) {
        if (plan == null) {
            log.warn("Sweep plan is null; no transaction created.");
            return Optional.empty();
        }

        if (!plan.sweepable()) {
            log.warn("Plan for nonce {} marked not sweepable; skipping send.", plan.nonce());
            return Optional.empty();
        }

        SweepRecord sweepRecord = null;
        try {
            sweepRecord = sweepRecordRepository.save(asPlannedRecord(plan));

            RawTransaction rawTransaction = RawTransaction.createEtherTransaction(
                    plan.nonce(),
                    plan.gasLimit(),
                    plan.to(),
                    plan.sweepAmountWei(),
                    plan.priorityFeeWei(),
                    plan.maxFeeWei());

            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, plan.chainId().longValue(), credentials);
            String hexPayload = Numeric.toHexString(signedMessage);

            EthSendTransaction response = web3j.ethSendRawTransaction(hexPayload).send();
            if (response == null) {
                log.error("eth_sendRawTransaction returned null response for sweep {}.", sweepRecord.getId());
                markFailed(sweepRecord);
                return Optional.empty();
            }

            if (response.hasError()) {
                log.error("Sweep {} rejected (code {}): {}",
                        sweepRecord.getId(),
                        response.getError().getCode(),
                        response.getError().getMessage());
                markFailed(sweepRecord);
                return Optional.empty();
            }

            String txHash = response.getTransactionHash();
            if (txHash == null) {
                log.error("Sweep {} succeeded without tx hash; marking as failed.", sweepRecord.getId());
                markFailed(sweepRecord);
                return Optional.empty();
            }

            markSent(sweepRecord, txHash);
            log.info("Sweep sent from {} to {} amount {} Wei ({} ETH) gasLimit {} maxFee {} gwei priorityFee {} gwei txHash {}",
                    sweepRecord.getId(),
                    plan.from(),
                    plan.to(),
                    plan.sweepAmountWei(),
                    toEth(plan.sweepAmountWei()),
                    plan.gasLimit(),
                    toGwei(plan.maxFeeWei()),
                    toGwei(plan.priorityFeeWei()),
                    txHash);
            return Optional.of(txHash);
        } catch (IOException ex) {
            log.error("RPC error while sending sweep transaction for sweep {}: {}",
                    sweepRecord != null ? sweepRecord.getId() : "n/a",
                    ex.getMessage());
            markFailed(sweepRecord);
            log.debug("Sweep transaction failure", ex);
            return Optional.empty();
        }
    }

    private BigDecimal toEth(BigInteger wei) {
        return scale(Convert.fromWei(new BigDecimal(wei), Convert.Unit.ETHER));
    }

    private BigDecimal toGwei(BigInteger wei) {
        return scale(Convert.fromWei(new BigDecimal(wei), Convert.Unit.GWEI));
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(9, RoundingMode.DOWN).stripTrailingZeros();
    }

    private SweepRecord asPlannedRecord(Plan plan) {
        SweepRecord record = new SweepRecord();
        record.setFromAddress(plan.from());
        record.setToAddress(plan.to());
        record.setAmountWei(plan.sweepAmountWei());
        record.setGasLimit(plan.gasLimit());
        record.setEffectiveFeePerGasWei(plan.effectiveFeePerGasWei());
        record.setGasCostWei(plan.gasCostWei());
        record.setNonce(plan.nonce());
        record.setChainId(plan.chainId());
        record.setTxHash(null);
        record.setStatus(SweepStatus.PLANNED);
        return record;
    }

    private void markFailed(SweepRecord record) {
        if (record == null) {
            return;
        }
        record.setStatus(SweepStatus.FAILED);
        record.setTxHash(null);
        sweepRecordRepository.save(record);
    }

    private void markSent(SweepRecord record, String txHash) {
        record.setTxHash(txHash);
        record.setStatus(SweepStatus.SENT);
        sweepRecordRepository.save(record);
    }
}