package com.ayrotek.pool_ser.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ayrotek.pool_ser.entity.SweepRecord;

@Repository
public interface SweepRecordRepository extends JpaRepository<SweepRecord, Long> {

    List<SweepRecord> findTop20ByOrderByCreatedAtDesc();
}
