package com.ayrotek.pool.link;

import com.ayrotek.pool.user.User;
import com.ayrotek.pool.user.UserRepository;
import com.ayrotek.pool.user.F2PoolAccount;
// ...existing code...
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MiningAccountLinkService {
    private final MiningAccountLinkRepository repo;
    private final UserRepository userRepo;

    public MiningAccountLinkService(MiningAccountLinkRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    public MiningAccountLink createLink(Long userId, F2PoolAccount f2, com.ayrotek.pool.minerstat.MinerstatAccount minerstat) {
        User user = userRepo.findById(userId).orElseThrow();
        MiningAccountLink link = new MiningAccountLink();
        link.setUser(user);
        link.setF2poolAccount(f2);
        link.setMinerstatAccount(minerstat);
        return repo.save(link);
    }

    public List<MiningAccountLink> getLinks(Long userId) {
        return repo.findByUserId(userId);
    }

    public void deleteLink(Long linkId) {
        repo.deleteById(linkId);
    }
}
