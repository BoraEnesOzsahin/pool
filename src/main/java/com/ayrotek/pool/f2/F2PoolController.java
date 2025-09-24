package com.ayrotek.pool.f2;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/f2pool")
public class F2PoolController {
private final F2PoolService service;
public F2PoolController(F2PoolService service) { this.service = service; }


@GetMapping("/accounts")
public ResponseEntity<JsonNode> listAccounts() { return ResponseEntity.ok(service.listMiningUsers()); }


@GetMapping("/accounts/{name}")
public ResponseEntity<JsonNode> getAccount(@PathVariable String name) { return ResponseEntity.ok(service.getMiningUser(name)); }


@GetMapping("/{currency}/accounts/{name}/workers")
public ResponseEntity<JsonNode> workers(@PathVariable String currency, @PathVariable String name) {
return ResponseEntity.ok(service.listWorkers(name, currency));
}


@GetMapping("/{currency}/accounts/{name}/hashrate")
public ResponseEntity<JsonNode> accountHistory(@PathVariable String currency, @PathVariable String name,
@RequestParam(defaultValue = "600") long interval,
@RequestParam(defaultValue = "86400") long duration) {
return ResponseEntity.ok(service.accountHashrateHistory(name, currency, interval, duration));
}


@GetMapping("/{currency}/accounts/{name}/workers/{worker}/history")
public ResponseEntity<JsonNode> workerHistory(@PathVariable String currency, @PathVariable String name,
@PathVariable String worker,
@RequestParam(defaultValue = "600") long interval,
@RequestParam(defaultValue = "86400") long duration) {
return ResponseEntity.ok(service.workerHashrateHistory(name, currency, worker, interval, duration));
}
}