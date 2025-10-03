package com.ayrotek.pool.user;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class F2PoolAccountService {
    private final F2PoolAccountRepository repo;
    private final UserRepository userRepo;

    public F2PoolAccountService(F2PoolAccountRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    public F2PoolAccount addAccount(Long userId, String accountName, String apiSecret) {
        User user = userRepo.findById(userId).orElseThrow();
        F2PoolAccount acc = new F2PoolAccount();
        acc.setUser(user);
        acc.setAccountName(accountName);
        acc.setApiSecret(apiSecret);
        return repo.save(acc);
    }

    public List<F2PoolAccount> getAccounts(Long userId) {
        return repo.findByUserId(userId);
    }
}
