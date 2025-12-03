package com.ayrotek.pool_ser.entity;

import java.math.BigInteger;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "sweeps")
public class SweepRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_address", nullable = false, length = 128)
    private String fromAddress;

    @Column(name = "to_address", nullable = false, length = 128)
    private String toAddress;

    @Column(name = "amount_wei", nullable = false)
    private BigInteger amountWei;

    @Column(name = "gas_limit", nullable = false)
    private BigInteger gasLimit;

    @Column(name = "effective_fee_per_gas_wei", nullable = false)
    private BigInteger effectiveFeePerGasWei;

    @Column(name = "gas_cost_wei", nullable = false)
    private BigInteger gasCostWei;

    @Column(name = "nonce", nullable = false)
    private BigInteger nonce;

    @Column(name = "chain_id", nullable = false)
    private BigInteger chainId;

    @Column(name = "tx_hash", length = 128)
    private String txHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private SweepStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "last_error", length = 512)
    private String lastError;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public BigInteger getAmountWei() {
        return amountWei;
    }

    public void setAmountWei(BigInteger amountWei) {
        this.amountWei = amountWei;
    }

    public BigInteger getGasLimit() {
        return gasLimit;
    }

    public void setGasLimit(BigInteger gasLimit) {
        this.gasLimit = gasLimit;
    }

    public BigInteger getEffectiveFeePerGasWei() {
        return effectiveFeePerGasWei;
    }

    public void setEffectiveFeePerGasWei(BigInteger effectiveFeePerGasWei) {
        this.effectiveFeePerGasWei = effectiveFeePerGasWei;
    }

    public BigInteger getGasCostWei() {
        return gasCostWei;
    }

    public void setGasCostWei(BigInteger gasCostWei) {
        this.gasCostWei = gasCostWei;
    }

    public BigInteger getNonce() {
        return nonce;
    }

    public void setNonce(BigInteger nonce) {
        this.nonce = nonce;
    }

    public BigInteger getChainId() {
        return chainId;
    }

    public void setChainId(BigInteger chainId) {
        this.chainId = chainId;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public SweepStatus getStatus() {
        return status;
    }

    public void setStatus(SweepStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(Instant lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }
}
