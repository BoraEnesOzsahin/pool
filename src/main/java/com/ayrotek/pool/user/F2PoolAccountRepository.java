package com.ayrotek.pool.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface F2PoolAccountRepository extends JpaRepository<F2PoolAccount, Long> {
    List<F2PoolAccount> findByUserId(Long userId);
}
