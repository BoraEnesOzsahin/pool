package com.example.miner.service;

import com.example.miner.entity.WorkerEntity;
import com.example.miner.repository.WorkerRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "app.subaccount=testSubaccount",
    "app.workerNamePrefix=w",
    "app.defaultPool=antpool"
})
class WorkerServiceTests {

    @Autowired
    private WorkerService workerService;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private WorkerRepo workerRepo;

    @Test
    void testCreateWorker_GeneratesCorrectWorkerNameFormat() {
        // When
        WorkerService.WorkerCreationResult result = workerService.create(null, null, "w", null);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.getWorkerName());
        assertNotNull(result.getPassword());
        
        // Verify worker name format: subaccount.prefix-uuid
        assertTrue(result.getWorkerName().matches("^[A-Za-z0-9_-]{1,64}\\.[A-Za-z0-9_-]{1,64}$"),
            "Worker name should match AntPool format");
        assertTrue(result.getWorkerName().startsWith("testSubaccount.w-"),
            "Worker name should start with subaccount.prefix-");
        
        // Verify password is 16 characters
        assertEquals(16, result.getPassword().length(),
            "Password should be 16 characters");
    }

    @Test
    void testCreateWorker_WithCustomWorkerName() {
        // When
        WorkerService.WorkerCreationResult result = workerService.create(
            "myAccount", "gpu01", null, null);
        
        // Then
        assertEquals("myAccount.gpu01", result.getWorkerName(),
            "Worker name should use custom values");
    }

    @Test
    void testCreateWorker_WithCustomPassword() {
        // When
        String customPassword = "MyCustomPass123!";
        WorkerService.WorkerCreationResult result = workerService.create(
            null, "worker1", null, customPassword);
        
        // Then
        assertEquals(customPassword, result.getPassword(),
            "Should use custom password");
        
        // Verify it's hashed in database
        WorkerEntity worker = workerRepo.findByWorkerName(result.getWorkerName()).orElseThrow();
        assertTrue(passwordService.verifyPassword(worker.getPasswordHash(), customPassword),
            "Custom password should be properly hashed");
    }

    @Test
    void testCreateWorker_StoresArgon2Hash() {
        // When
        WorkerService.WorkerCreationResult result = workerService.create(null, null, "test", null);
        
        // Then
        WorkerEntity savedWorker = workerRepo.findByWorkerName(result.getWorkerName())
            .orElseThrow();
        
        // Verify password is hashed (Argon2 hashes start with $argon2)
        assertTrue(savedWorker.getPasswordHash().startsWith("$argon2"),
            "Password should be stored as Argon2 hash");
        
        // Verify hash can be verified with plaintext password
        assertTrue(passwordService.verifyPassword(
            savedWorker.getPasswordHash(), 
            result.getPassword()),
            "Stored hash should verify against plaintext password");
    }

    @Test
    void testCreateWorker_SetsDefaultValues() {
        // When
        WorkerService.WorkerCreationResult result = workerService.create(null, null, "w", null);
        
        // Then
        WorkerEntity worker = workerRepo.findByWorkerName(result.getWorkerName())
            .orElseThrow();
        
        assertEquals("antpool", worker.getPool());
        assertEquals(WorkerEntity.WorkerStatus.ENABLED, worker.getStatus());
        assertNotNull(worker.getCreatedAt());
        assertNotNull(worker.getLastIssuedAt());
    }

    @Test
    void testCreateWorker_WithCustomSubaccount() {
        // When
        WorkerService.WorkerCreationResult result = workerService.create("myCustomAccount", null, "miner", null);
        
        // Then
        assertNotNull(result);
        assertTrue(result.getWorkerName().startsWith("myCustomAccount.miner-"),
            "Worker name should use custom subaccount");
    }

    @Test
    void testDisableWorker_UpdatesStatus() {
        // Given
        WorkerService.WorkerCreationResult result = workerService.create(null, null, "w", null);
        WorkerEntity worker = workerRepo.findByWorkerName(result.getWorkerName())
            .orElseThrow();
        
        // When
        workerService.disable(worker.getId());
        
        // Then
        WorkerEntity disabled = workerRepo.findById(worker.getId()).orElseThrow();
        assertEquals(WorkerEntity.WorkerStatus.DISABLED, disabled.getStatus());
    }
}
