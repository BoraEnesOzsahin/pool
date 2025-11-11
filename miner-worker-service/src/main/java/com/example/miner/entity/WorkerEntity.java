package com.example.miner.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workers")
public class WorkerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}\\.[A-Za-z0-9_-]{1,64}$", 
             message = "Worker name must follow format: subaccount.workername")
    @Column(unique = true, nullable = false, length = 130)
    private String workerName;

    @NotBlank
    @Column(nullable = false, length = 500)
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String pool = "antpool";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkerStatus status = WorkerStatus.ENABLED;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime lastIssuedAt;

    // Constructors
    public WorkerEntity() {
    }

    public WorkerEntity(String workerName, String passwordHash, String pool) {
        this.workerName = workerName;
        this.passwordHash = passwordHash;
        this.pool = pool;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getWorkerName() {
        return workerName;
    }

    public void setWorkerName(String workerName) {
        this.workerName = workerName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPool() {
        return pool;
    }

    public void setPool(String pool) {
        this.pool = pool;
    }

    public WorkerStatus getStatus() {
        return status;
    }

    public void setStatus(WorkerStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastIssuedAt() {
        return lastIssuedAt;
    }

    public void setLastIssuedAt(LocalDateTime lastIssuedAt) {
        this.lastIssuedAt = lastIssuedAt;
    }

    public enum WorkerStatus {
        ENABLED,
        DISABLED
    }
}
