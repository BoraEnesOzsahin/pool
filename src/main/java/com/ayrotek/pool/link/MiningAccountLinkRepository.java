package com.ayrotek.pool.link;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MiningAccountLinkRepository extends JpaRepository<MiningAccountLink, Long> {
    List<MiningAccountLink> findByUserId(Long userId);
}
