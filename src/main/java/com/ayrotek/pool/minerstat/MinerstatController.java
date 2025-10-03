package com.ayrotek.pool.minerstat;

import com.ayrotek.pool.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/minerstat")
@Tag(name = "Minerstat", description = "Endpoints for Minerstat monitoring and control")
public class MinerstatController {
    private final MinerstatApiService apiService;
    private final MinerstatAccountRepository accountRepo;
    private final UserRepository userRepo;
    @PostMapping("/accounts")
    @Operation(summary = "Add Minerstat account", description = "Add a Minerstat account (API key and worker name) for the authenticated user.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> addMinerstatAccount(@RequestBody MinerstatAccountRequest req, Authentication auth) {
        String username = (String) auth.getPrincipal();
        Long userId = userRepo.findByUsername(username).orElseThrow().getId();
        if (req.apiKey == null || req.apiKey.isBlank() || req.worker == null || req.worker.isBlank()) {
            return ResponseEntity.badRequest().body("API key and worker name are required");
        }
        MinerstatAccount acc = new MinerstatAccount();
        acc.setApiKey(req.apiKey);
        acc.setWorker(req.worker);
        acc.setUser(userRepo.findById(userId).orElseThrow());
        accountRepo.save(acc);
        return ResponseEntity.status(HttpStatus.CREATED).body(acc);
    }

    public static class MinerstatAccountRequest {
        public String apiKey;
        public String worker;
    }

    public MinerstatController(MinerstatApiService apiService, MinerstatAccountRepository accountRepo, UserRepository userRepo) {
        this.apiService = apiService;
        this.accountRepo = accountRepo;
        this.userRepo = userRepo;
    }

    @GetMapping("/workers")
    @Operation(summary = "Get all workers", description = "Fetch all workers for the authenticated user's Minerstat account.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> getAllWorkers(Authentication auth) {
        String username = (String) auth.getPrincipal();
        Long userId = userRepo.findByUsername(username).orElseThrow().getId();
        Optional<MinerstatAccount> accOpt = accountRepo.findByUserId(userId).stream().findFirst();
        if (accOpt.isEmpty()) return ResponseEntity.notFound().build();
        MinerstatAccount acc = accOpt.get();
        Map<String, Object> result = apiService.getAllWorkers(acc.getApiKey());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/workers/{worker}")
    @Operation(summary = "Get worker stats", description = "Fetch stats for a specific worker.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> getWorkerStats(@PathVariable String worker, Authentication auth) {
        String username = (String) auth.getPrincipal();
        Long userId = userRepo.findByUsername(username).orElseThrow().getId();
        Optional<MinerstatAccount> accOpt = accountRepo.findByUserId(userId).stream().filter(a -> a.getWorker().equals(worker)).findFirst();
        if (accOpt.isEmpty()) return ResponseEntity.notFound().build();
        MinerstatAccount acc = accOpt.get();
        Map<String, Object> result = apiService.getWorkerStats(acc.getApiKey(), worker);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/workers/{worker}/command/{command}")
    @Operation(summary = "Send command to worker", description = "Send a command (start, stop, reboot, etc.) to a specific worker.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> sendWorkerCommand(@PathVariable String worker, @PathVariable String command, Authentication auth) {
        String username = (String) auth.getPrincipal();
        Long userId = userRepo.findByUsername(username).orElseThrow().getId();
        Optional<MinerstatAccount> accOpt = accountRepo.findByUserId(userId).stream().filter(a -> a.getWorker().equals(worker)).findFirst();
        if (accOpt.isEmpty()) return ResponseEntity.notFound().build();
        MinerstatAccount acc = accOpt.get();
        Map<String, Object> result = apiService.sendWorkerCommand(acc.getApiKey(), worker, command);
        return ResponseEntity.ok(result);
    }
}
