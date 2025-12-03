package com.ayrotek.pool_ser.repository;

import java.util.List;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ayrotek.pool_ser.entity.SweepRecord;
import com.ayrotek.pool_ser.entity.SweepStatus;

@Repository
public interface SweepRecordRepository extends JpaRepository<SweepRecord, Long> {

    List<SweepRecord> findTop20ByOrderByCreatedAtDesc();

    List<SweepRecord> findTop100ByOrderByCreatedAtDesc();

    List<SweepRecord> findTop50ByStatusOrderByCreatedAtDesc(SweepStatus status);

    List<SweepRecord> findTop50ByStatusInAndTxHashIsNotNullOrderByCreatedAtAsc(Collection<SweepStatus> statuses);
}
