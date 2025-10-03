package com.ayrotek.pool.link;

import com.ayrotek.pool.user.F2PoolAccount;
import com.ayrotek.pool.minerstat.MinerstatAccount;
import com.ayrotek.pool.minerstat.MinerstatAccountRepository;
import com.ayrotek.pool.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/mining-links")
public class MiningAccountLinkController {
    private final MiningAccountLinkService service;
    private final UserRepository userRepo;
    private final MinerstatAccountRepository minerstatRepo;

    public MiningAccountLinkController(MiningAccountLinkService service, UserRepository userRepo, MinerstatAccountRepository minerstatRepo) {
        this.service = service;
        this.userRepo = userRepo;
        this.minerstatRepo = minerstatRepo;
    }

    @PostMapping
    public ResponseEntity<MiningAccountLink> createLink(@RequestBody LinkRequest req, Authentication auth) {
        String username = (String) auth.getPrincipal();
        Long userId = userRepo.findByUsername(username).orElseThrow().getId();
        if (req.f2poolAccount == null || req.minerstatAccount == null) {
            return ResponseEntity.badRequest().build();
        }
        // Optionally, fetch managed entities from DB if only IDs are provided
        MiningAccountLink link = service.createLink(userId, req.f2poolAccount, req.minerstatAccount);
        return ResponseEntity.ok(link);
    }

    @GetMapping
    public ResponseEntity<List<MiningAccountLink>> getLinks(Authentication auth) {
        String username = (String) auth.getPrincipal();
        Long userId = userRepo.findByUsername(username).orElseThrow().getId();
        return ResponseEntity.ok(service.getLinks(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLink(@PathVariable Long id) {
        service.deleteLink(id);
        return ResponseEntity.noContent().build();
    }

    public static class LinkRequest {
        public F2PoolAccount f2poolAccount;
        public MinerstatAccount minerstatAccount;
    }
}
