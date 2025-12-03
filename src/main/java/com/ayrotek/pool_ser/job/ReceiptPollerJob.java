package com.ayrotek.pool_ser.job;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import com.ayrotek.pool_ser.entity.SweepRecord;
import com.ayrotek.pool_ser.entity.SweepStatus;
import com.ayrotek.pool_ser.repository.SweepRecordRepository;

@Component
public class ReceiptPollerJob {

    private static final Logger log = LoggerFactory.getLogger(ReceiptPollerJob.class);

    private static final String RETRY_NOT_IMPLEMENTED_MESSAGE = "Retry planned but not implemented; marking as FAILED";

    private final Web3j web3j;
    private final SweepRecordRepository repository;
    private final boolean pollingEnabled;
    private final int receiptMaxRetries;

    public ReceiptPollerJob(Web3j web3j,
            SweepRecordRepository repository,
            @Value("${SWEEPER_RECEIPT_POLL_ENABLED:true}") String pollingEnabledValue,
            @Value("${SWEEPER_RECEIPT_MAX_RETRIES:3}") int receiptMaxRetries) {
        this.web3j = web3j;
        this.repository = repository;
        this.pollingEnabled = Boolean.parseBoolean(pollingEnabledValue);
        this.receiptMaxRetries = receiptMaxRetries;
    }

    @Scheduled(fixedDelayString = "${SWEEPER_RECEIPT_POLL_MILLIS:60000}")
    public void poll() {
        if (!pollingEnabled) {
            log.debug("Receipt polling disabled via SWEEPER_RECEIPT_POLL_ENABLED");
            return;
        }

        List<SweepStatus> statusesToCheck = List.of(SweepStatus.SENT, SweepStatus.RETRY_PLANNED);
        List<SweepRecord> candidates = repository.findTop50ByStatusInAndTxHashIsNotNullOrderByCreatedAtAsc(statusesToCheck);
        if (candidates.isEmpty()) {
            return;
        }

        for (SweepRecord record : candidates) {
            try {
                processRecord(record);
            } catch (Exception ex) {
                log.error("Receipt polling failed for sweep {}: {}", record.getId(), ex.getMessage());
                log.debug("Receipt polling exception", ex);
            }
        }
    }

    private void processRecord(SweepRecord record) throws IOException {
        if (record.getTxHash() == null) {
            return;
        }

        if (record.getRetryCount() >= receiptMaxRetries) {
            markFailed(record, "Retry count exceeded max retries");
            return;
        }

        if (record.getStatus() == SweepStatus.RETRY_PLANNED) {
            markFailed(record, RETRY_NOT_IMPLEMENTED_MESSAGE);
            return;
        }

        EthGetTransactionReceipt response = web3j.ethGetTransactionReceipt(record.getTxHash()).send();
        if (response == null) {
            log.warn("eth_getTransactionReceipt returned null for sweep {}", record.getId());
            touchRecord(record, null);
            return;
        }

        if (response.hasError()) {
            log.warn("Receipt polling error for sweep {}: {}", record.getId(), response.getError().getMessage());
            touchRecord(record, response.getError().getMessage());
            return;
        }

        if (response.getTransactionReceipt().isEmpty()) {
            touchRecord(record, null);
            return;
        }

        TransactionReceipt receipt = response.getTransactionReceipt().get();
        if (receipt.isStatusOK()) {
            record.setStatus(SweepStatus.CONFIRMED);
            record.setLastError(null);
            record.setLastCheckedAt(Instant.now());
            repository.save(record);
            log.info("Sweep {} confirmed on-chain with txHash {}", record.getId(), record.getTxHash());
        } else {
            markReverted(record);
        }
    }

    private void markReverted(SweepRecord record) {
        record.setStatus(SweepStatus.REVERTED);
        record.setLastError("On-chain revert");
        record.setLastCheckedAt(Instant.now());
        repository.save(record);
        log.warn("Sweep {} reverted on-chain", record.getId());
    }

    private void markFailed(SweepRecord record, String reason) {
        record.setStatus(SweepStatus.FAILED);
        record.setLastError(truncate(reason));
        record.setLastCheckedAt(Instant.now());
        repository.save(record);
    }

    private void touchRecord(SweepRecord record, String message) {
        record.setLastCheckedAt(Instant.now());
        if (message != null) {
            record.setLastError(truncate(message));
        }
        repository.save(record);
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 512 ? value : value.substring(0, 512);
    }
}
