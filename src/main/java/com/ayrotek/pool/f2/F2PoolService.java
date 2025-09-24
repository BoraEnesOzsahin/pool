package com.ayrotek.pool.f2;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class F2PoolService {
    private final F2PoolClient client;

    public F2PoolService(F2PoolClient client) {
        this.client = client;
    }

    public JsonNode listMiningUsers() {
        return client.post("/mining_user/list", new HashMap<>());
    }

    public JsonNode getMiningUser(String miningUserName) {
        var body = new HashMap<String, Object>();
        body.put("mining_user_name", miningUserName);
        return client.post("/mining_user/get", body);
    }

    public JsonNode listWorkers(String miningUserName, String currency) {
        var body = new HashMap<String, Object>();
        body.put("mining_user_name", miningUserName);
        body.put("currency", currency);
        return client.post("/hash_rate/worker/list", body);
    }

    public JsonNode accountHashrateHistory(String miningUserName, String currency, long interval, long duration) {
        var body = new HashMap<String, Object>();
        body.put("mining_user_name", miningUserName);
        body.put("currency", currency);
        body.put("interval", interval);
        body.put("duration", duration);
        return client.post("/hash_rate/history", body);
    }

    public JsonNode workerHashrateHistory(String miningUserName, String currency, String workerName, long interval, long duration) {
        var body = new HashMap<String, Object>();
        body.put("mining_user_name", miningUserName);
        body.put("currency", currency);
        body.put("worker_name", workerName);
        body.put("interval", interval);
        body.put("duration", duration);
        return client.post("/hash_rate/worker/history", body);
    }
}
