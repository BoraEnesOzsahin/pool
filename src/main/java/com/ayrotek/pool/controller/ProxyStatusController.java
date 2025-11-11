package com.ayrotek.pool.controller;

import com.ayrotek.pool.stratum.StratumServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/proxy")
public class ProxyStatusController {

    @Autowired
    private StratumServer stratumServer;

    @Value("${stratum.server.host:0.0.0.0}")
    private String serverHost;

    @Value("${stratum.server.port:3333}")
    private int serverPort;

    @Value("${stratum.upstream.host:xmr.antpool.com}")
    private String upstreamHost;

    @Value("${stratum.upstream.port:9005}")
    private int upstreamPort;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("proxy_status", "running");
        status.put("active_connections", stratumServer.getActiveConnections());
        
        Map<String, String> listening = new LinkedHashMap<>();
        listening.put("host", serverHost);
        listening.put("port", String.valueOf(serverPort));
        status.put("listening", listening);
        
        Map<String, String> upstream = new LinkedHashMap<>();
        upstream.put("host", upstreamHost);
        upstream.put("port", String.valueOf(upstreamPort));
        status.put("upstream_pool", upstream);
        
        return ResponseEntity.ok(status);
    }
}
