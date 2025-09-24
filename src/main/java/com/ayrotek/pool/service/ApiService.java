
/*package com.ayrotek.pool.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;

@Service
public class ApiService {

    @Value("${f2pool.api.token}")
    private String apiToken; // F2Pool API token

    @Value("${f2pool.account.name}")
    private String accountName; // F2Pool account name

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Get account info
    public String getAccountInfo() {
        String url = "https://api.f2pool.com/v2/mining_user/get";
        HashMap<String, Object> body = new HashMap<>();
        body.put("mining_user_name", accountName);
        return postJson(url, body);
    }

    // Get Litecoin balance
    public String getLitecoinBalance() {
        String url = "https://api.f2pool.com/v2/assets/balance";
        HashMap<String, Object> body = new HashMap<>();
        body.put("currency", "litecoin");
        body.put("mining_user_name", accountName);
        return postJson(url, body);
    }

    // Get Litecoin hashrate info
    public String getLitecoinHashrateInfo() {
        String url = "https://api.f2pool.com/v2/hash_rate/info";
        HashMap<String, Object> body = new HashMap<>();
        body.put("currency", "litecoin");
        body.put("mining_user_name", accountName);
        return postJson(url, body);
    }

    // Get Litecoin worker list
    public String getLitecoinWorkerList() {
        String url = "https://api.f2pool.com/v2/hash_rate/worker/list";
        HashMap<String, Object> body = new HashMap<>();
        body.put("currency", "litecoin");
        body.put("mining_user_name", accountName);
        return postJson(url, body);
    }

    // Get Litecoin blocks list (first page)
    public String getLitecoinBlocksList() {
        String url = "https://api.f2pool.com/v2/blocks/paging";
        HashMap<String, Object> body = new HashMap<>();
        body.put("currency", "litecoin");
        body.put("page", 1);
        body.put("pagesize", 20);
        return postJson(url, body);
    }

    // Helper for POST JSON with F2Pool API token
    private String postJson(String url, HashMap<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("F2P-API-SECRET", apiToken);
            String json = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            System.out.println("[DEBUG] F2Pool Request:");
            System.out.println("  URL: " + url);
            System.out.println("  Headers: " + headers);
            System.out.println("  Body: " + json);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            System.out.println("[DEBUG] F2Pool Response:");
            System.out.println("  Status: " + response.getStatusCode());
            System.out.println("  Body: " + response.getBody());
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("F2Pool API request failed", e);
        }
    }
}*/