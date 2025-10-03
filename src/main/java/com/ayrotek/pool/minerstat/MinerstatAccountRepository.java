package com.ayrotek.pool.minerstat;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MinerstatAccountRepository extends JpaRepository<MinerstatAccount, Long> {
    List<MinerstatAccount> findByUserId(Long userId);
}
