package com.ayrotek.pool.minerstat;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class MinerstatApiService {
    private static final String BASE_URL = "https://api.minerstat.com/v2";
    private final RestTemplate restTemplate = new RestTemplate();


    public Map<String, Object> getWorkerStats(String apiKey, String worker) {
        String url = BASE_URL + "/stats/" + apiKey + "/" + worker;
        ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(url, (Class<Map<String, Object>>) (Class<?>) Map.class);
        return response.getBody();
    }


    public Map<String, Object> getAllWorkers(String apiKey) {
        String url = BASE_URL + "/stats/" + apiKey;
        ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(url, (Class<Map<String, Object>>) (Class<?>) Map.class);
        return response.getBody();
    }


    public Map<String, Object> sendWorkerCommand(String apiKey, String worker, String command) {
        String url = BASE_URL + "/worker/" + apiKey + "/" + worker + "/" + command;
        ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(url, (Class<Map<String, Object>>) (Class<?>) Map.class);
        return response.getBody();
    }
}
