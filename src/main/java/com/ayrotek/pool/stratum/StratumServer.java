package com.ayrotek.pool.stratum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stratum Protocol TCP Server
 * Listens for miner connections and forwards shares to upstream pool
 */
@Component
public class StratumServer {

    private static final Logger log = LoggerFactory.getLogger(StratumServer.class);

    @Value("${stratum.server.host:0.0.0.0}")
    private String serverHost;

    @Value("${stratum.server.port:3333}")
    private int serverPort;

    @Value("${stratum.upstream.host:xmr.antpool.com}")
    private String upstreamHost;

    @Value("${stratum.upstream.port:9005}")
    private int upstreamPort;

    private AsynchronousServerSocketChannel serverChannel;
    private final ConcurrentHashMap<String, MinerConnection> minerConnections = new ConcurrentHashMap<>();
    private final AtomicLong connectionCounter = new AtomicLong(0);

    @PostConstruct
    public void start() throws IOException {
        serverChannel = AsynchronousServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(serverHost, serverPort));
        
        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║   STRATUM PROXY SERVER STARTED                             ║");
        log.info("╠════════════════════════════════════════════════════════════╣");
        log.info("║  Listening: {}:{}", String.format("%-10s", serverHost), serverPort);
        log.info("║  Upstream:  {}:{}", String.format("%-15s", upstreamHost), upstreamPort);
        log.info("╚════════════════════════════════════════════════════════════╝");
        
        acceptConnections();
    }

    private void acceptConnections() {
        serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel clientChannel, Void attachment) {
                // Accept next connection immediately
                serverChannel.accept(null, this);
                
                // Handle this connection
                long connId = connectionCounter.incrementAndGet();
                String clientId = "miner-" + connId;
                
                try {
                    String remoteAddress = clientChannel.getRemoteAddress().toString();
                    log.info("✓ New connection #{}: {} -> Establishing upstream", connId, remoteAddress);
                    
                    MinerConnection minerConn = new MinerConnection(
                        clientId, 
                        clientChannel, 
                        upstreamHost, 
                        upstreamPort
                    );
                    
                    minerConnections.put(clientId, minerConn);
                    minerConn.start();
                    
                } catch (Exception e) {
                    log.error("✗ Failed to handle connection #{}: {}", connId, e.getMessage());
                    try {
                        clientChannel.close();
                    } catch (IOException ignored) {}
                }
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                log.error("Failed to accept connection: {}", exc.getMessage());
                // Continue accepting connections
                acceptConnections();
            }
        });
    }

    @PreDestroy
    public void stop() {
        log.info("Shutting down Stratum server...");
        
        // Close all miner connections
        minerConnections.values().forEach(MinerConnection::close);
        minerConnections.clear();
        
        // Close server socket
        if (serverChannel != null && serverChannel.isOpen()) {
            try {
                serverChannel.close();
                log.info("✓ Server stopped successfully");
            } catch (IOException e) {
                log.error("Error closing server socket", e);
            }
        }
    }

    public int getActiveConnections() {
        return minerConnections.size();
    }
}
