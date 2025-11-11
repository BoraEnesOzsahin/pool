package com.example.miner.repository;

import com.example.miner.entity.WorkerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkerRepo extends JpaRepository<WorkerEntity, UUID> {
    
    Optional<WorkerEntity> findByWorkerName(String workerName);
}
