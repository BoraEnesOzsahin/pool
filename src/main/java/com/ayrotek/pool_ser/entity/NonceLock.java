package com.ayrotek.pool_ser.entity;

import java.math.BigInteger;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "nonce_locks")
public class NonceLock {

    @Id
    @Column(name = "address", nullable = false, updatable = false, length = 64)
    private String address;

    @Column(name = "last_nonce", nullable = false)
    private BigInteger lastNonce;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BigInteger getLastNonce() {
        return lastNonce;
    }

    public void setLastNonce(BigInteger lastNonce) {
        this.lastNonce = lastNonce;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
