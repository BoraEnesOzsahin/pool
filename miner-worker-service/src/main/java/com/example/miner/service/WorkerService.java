package com.example.miner.service;

import com.example.miner.entity.WorkerEntity;
import com.example.miner.repository.WorkerRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class WorkerService {

    private final WorkerRepo workerRepo;
    private final PasswordService passwordService;

    @Value("${app.subaccount}")
    private String subaccount;

    @Value("${app.defaultPool}")
    private String defaultPool;

    public WorkerService(WorkerRepo workerRepo, PasswordService passwordService) {
        this.workerRepo = workerRepo;
        this.passwordService = passwordService;
    }

    /**
     * Create a new worker with full customization
     * @param customSubaccount Optional custom subaccount (if null, uses configured subaccount)
     * @param customWorkerName Optional custom worker name (if provided, used as-is; otherwise auto-generated)
     * @param prefix Prefix for auto-generated name (only used if customWorkerName is null)
     * @param customPassword Optional custom password (if null, generates random password)
     * @return WorkerCreationResult containing worker name and plaintext password
     */
    @Transactional
    public WorkerCreationResult create(String customSubaccount, String customWorkerName, 
                                      String prefix, String customPassword) {
        // Use custom subaccount if provided, otherwise use default
        String accountToUse = (customSubaccount != null && !customSubaccount.isBlank()) 
            ? customSubaccount 
            : subaccount;
        
        // Build full worker name
        String fullWorkerName;
        if (customWorkerName != null && !customWorkerName.isBlank()) {
            // User provided custom worker name
            fullWorkerName = accountToUse + "." + customWorkerName;
        } else {
            // Auto-generate with prefix and UUID
            String uuidSuffix = UUID.randomUUID().toString().substring(0, 8);
            fullWorkerName = accountToUse + "." + prefix + "-" + uuidSuffix;
        }
        
        // Use custom password if provided, otherwise generate random
        String plaintextPassword = (customPassword != null && !customPassword.isBlank())
            ? customPassword
            : passwordService.generateRandomPassword();
        
        // Hash password
        String passwordHash = passwordService.hashPassword(plaintextPassword);
        
        // Create and save entity
        WorkerEntity worker = new WorkerEntity(fullWorkerName, passwordHash, defaultPool);
        worker.setLastIssuedAt(LocalDateTime.now());
        workerRepo.save(worker);
        
        return new WorkerCreationResult(fullWorkerName, plaintextPassword);
    }

    /**
     * Disable a worker by ID
     */
    @Transactional
    public void disable(UUID id) {
        WorkerEntity worker = workerRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Worker not found: " + id));
        worker.setStatus(WorkerEntity.WorkerStatus.DISABLED);
        workerRepo.save(worker);
    }

    /**
     * Result DTO for worker creation (contains plaintext password - use once!)
     */
    public static class WorkerCreationResult {
        private final String workerName;
        private final String password;

        public WorkerCreationResult(String workerName, String password) {
            this.workerName = workerName;
            this.password = password;
        }

        public String getWorkerName() {
            return workerName;
        }

        public String getPassword() {
            return password;
        }
    }
}
