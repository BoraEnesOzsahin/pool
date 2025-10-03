
package com.ayrotek.pool.f2;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class F2PoolAgentService {
    private static final Logger logger = LoggerFactory.getLogger(F2PoolAgentService.class);
    private final F2PoolService f2PoolService;

    private volatile AgentStatus latestStatus = new AgentStatus();
    public AgentStatus getLatestStatus() {
        return latestStatus;
    }

    @Value("${agent.litecoin.hashrate-threshold:100}")
    private double hashrateThreshold;

    public F2PoolAgentService(F2PoolService f2PoolService) {
        this.f2PoolService = f2PoolService;
    }

    // Runs every 5 minutes
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void checkWorkerStatus() {
        String miningUser = f2PoolService.getDefaultMiningUser();
        String currency = "litecoin";
        try {
            JsonNode workers = f2PoolService.listWorkers(miningUser, currency);
            int online = 0, offline = 0;
            double totalHashrate = 0.0;
            List<String> offlineList = new java.util.ArrayList<>();
            List<String> underList = new java.util.ArrayList<>();
            if (workers.has("workers")) {
                for (JsonNode worker : workers.get("workers")) {
                    String name = worker.has("worker_name") ? worker.get("worker_name").asText() : "?";
                    int status = worker.has("status") ? worker.get("status").asInt() : -1;
                    double hashrate = worker.has("hashrate") ? worker.get("hashrate").asDouble() : 0.0;
                    totalHashrate += hashrate;
                    if (status == 0) {
                        online++;
                        if (hashrate < hashrateThreshold) {
                            underList.add(name + " (" + hashrate + ")");
                        }
                    } else {
                        offline++;
                        offlineList.add(name);
                    }
                }
            }
            logger.info("[Agent] Workers: {} online, {} offline, total hashrate: {} MH/s", online, offline, totalHashrate);
            if (offline > 0) {
                logger.warn("[Agent] Offline workers: {}", offlineList);
            }
            if (!underList.isEmpty()) {
                logger.warn("[Agent] Underperforming workers (<{} MH/s): {}", hashrateThreshold, underList);
            }
            latestStatus = new AgentStatus(Instant.now().toString(), online, offline, totalHashrate, offlineList, underList);
        } catch (Exception e) {
            logger.error("[Agent] Failed to check worker status: {}", e.getMessage(), e);
            latestStatus = new AgentStatus(Instant.now().toString(), 0, 0, 0.0, Collections.emptyList(), Collections.emptyList());
        }
    }

    public static class AgentStatus {
        public String lastCheck;
        public int online;
        public int offline;
        public double totalHashrate;
        public List<String> offlineWorkers;
        public List<String> underperformingWorkers;

        public AgentStatus() {
            this.lastCheck = null;
            this.online = 0;
            this.offline = 0;
            this.totalHashrate = 0.0;
            this.offlineWorkers = Collections.emptyList();
            this.underperformingWorkers = Collections.emptyList();
        }

        public AgentStatus(String lastCheck, int online, int offline, double totalHashrate, List<String> offlineWorkers, List<String> underperformingWorkers) {
            this.lastCheck = lastCheck;
            this.online = online;
            this.offline = offline;
            this.totalHashrate = totalHashrate;
            this.offlineWorkers = offlineWorkers;
            this.underperformingWorkers = underperformingWorkers;
        }
    }
}   
