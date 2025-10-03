package com.ayrotek.pool.user;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/f2pool-accounts")
public class F2PoolAccountController {
    private final F2PoolAccountService service;
    private final UserRepository userRepo;

    public F2PoolAccountController(F2PoolAccountService service, UserRepository userRepo) {
        this.service = service;
        this.userRepo = userRepo;
    }

    @PostMapping
    public ResponseEntity<F2PoolAccount> addAccount(@RequestBody F2PoolAccountRequest req, Authentication auth) {
        String username = (String) auth.getPrincipal();
        Long userId = userRepo.findByUsername(username).orElseThrow().getId();
        F2PoolAccount acc = service.addAccount(userId, req.accountName, req.apiSecret);
        return ResponseEntity.ok(acc);
    }

    @GetMapping
    public ResponseEntity<List<F2PoolAccount>> getAccounts(Authentication auth) {
        String username = (String) auth.getPrincipal();
        Long userId = userRepo.findByUsername(username).orElseThrow().getId();
        return ResponseEntity.ok(service.getAccounts(userId));
    }

    public static class F2PoolAccountRequest {
        public String accountName;
        public String apiSecret;
    }
}
