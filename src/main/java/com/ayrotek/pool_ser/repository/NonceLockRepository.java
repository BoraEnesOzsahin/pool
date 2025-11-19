package com.ayrotek.pool_ser.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ayrotek.pool_ser.entity.NonceLock;

import jakarta.persistence.LockModeType;

@Repository
public interface NonceLockRepository extends JpaRepository<NonceLock, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NonceLock n where n.address = :address")
    Optional<NonceLock> findForUpdate(@Param("address") String address);
}
