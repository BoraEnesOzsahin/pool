package com.ayrotek.pool.stratum;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a single miner connection
 * Handles bidirectional communication between miner and upstream pool
 */
public class MinerConnection {

    private static final Logger log = LoggerFactory.getLogger(MinerConnection.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final String minerId;
    private final AsynchronousSocketChannel minerChannel;
    private final AsynchronousSocketChannel upstreamChannel;
    private final String upstreamHost;
    private final int upstreamPort;
    
    private final ByteBuffer minerReadBuffer = ByteBuffer.allocate(8192);
    private final ByteBuffer upstreamReadBuffer = ByteBuffer.allocate(8192);
    private final StringBuilder minerMessageBuffer = new StringBuilder();
    private final StringBuilder upstreamMessageBuffer = new StringBuilder();
    
    private final AtomicLong messagesReceived = new AtomicLong(0);
    private final AtomicLong messagesForwarded = new AtomicLong(0);
    private volatile boolean active = true;

    public MinerConnection(String minerId, AsynchronousSocketChannel minerChannel,
                          String upstreamHost, int upstreamPort) throws IOException {
        this.minerId = minerId;
        this.minerChannel = minerChannel;
        this.upstreamHost = upstreamHost;
        this.upstreamPort = upstreamPort;
        this.upstreamChannel = AsynchronousSocketChannel.open();
    }

    public void start() {
        // Connect to upstream pool
        upstreamChannel.connect(new InetSocketAddress(upstreamHost, upstreamPort),
            null, new CompletionHandler<Void, Void>() {
                @Override
                public void completed(Void result, Void attachment) {
                    log.info("[{}] ✓ Connected to upstream pool {}:{}", 
                        minerId, upstreamHost, upstreamPort);
                    
                    // Start reading from both miner and upstream
                    readFromMiner();
                    readFromUpstream();
                }

                @Override
                public void failed(Throwable exc, Void attachment) {
                    log.error("[{}] ✗ Failed to connect to upstream: {}", 
                        minerId, exc.getMessage());
                    close();
                }
            });
    }

    private void readFromMiner() {
        if (!active) return;
        
        minerReadBuffer.clear();
        minerChannel.read(minerReadBuffer, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer bytesRead, Void attachment) {
                if (bytesRead == -1) {
                    log.info("[{}] Miner disconnected", minerId);
                    close();
                    return;
                }

                minerReadBuffer.flip();
                byte[] data = new byte[bytesRead];
                minerReadBuffer.get(data);
                String message = new String(data, StandardCharsets.UTF_8);
                
                minerMessageBuffer.append(message);
                processMessages(minerMessageBuffer, true);
                
                // Continue reading
                readFromMiner();
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                log.error("[{}] Miner read error: {}", minerId, exc.getMessage());
                close();
            }
        });
    }

    private void readFromUpstream() {
        if (!active) return;
        
        upstreamReadBuffer.clear();
        upstreamChannel.read(upstreamReadBuffer, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer bytesRead, Void attachment) {
                if (bytesRead == -1) {
                    log.info("[{}] Upstream disconnected", minerId);
                    close();
                    return;
                }

                upstreamReadBuffer.flip();
                byte[] data = new byte[bytesRead];
                upstreamReadBuffer.get(data);
                String message = new String(data, StandardCharsets.UTF_8);
                
                upstreamMessageBuffer.append(message);
                processMessages(upstreamMessageBuffer, false);
                
                // Continue reading
                readFromUpstream();
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                log.error("[{}] Upstream read error: {}", minerId, exc.getMessage());
                close();
            }
        });
    }

    private void processMessages(StringBuilder buffer, boolean fromMiner) {
        String bufferContent = buffer.toString();
        int newlinePos;
        
        while ((newlinePos = bufferContent.indexOf('\n')) != -1) {
            String jsonMessage = bufferContent.substring(0, newlinePos).trim();
            buffer.delete(0, newlinePos + 1);
            bufferContent = buffer.toString();
            
            if (!jsonMessage.isEmpty()) {
                handleStratumMessage(jsonMessage, fromMiner);
            }
        }
    }

    private void handleStratumMessage(String jsonMessage, boolean fromMiner) {
        try {
            JsonNode json = mapper.readTree(jsonMessage);
            String method = json.has("method") ? json.get("method").asText() : "response";
            
            if (fromMiner) {
                messagesReceived.incrementAndGet();
                
                if ("mining.subscribe".equals(method)) {
                    log.info("[{}] ← mining.subscribe (miner)", minerId);
                } else if ("mining.authorize".equals(method)) {
                    JsonNode params = json.get("params");
                    String worker = params.get(0).asText();
                    log.info("[{}] ← mining.authorize: {}", minerId, worker);
                } else if ("mining.submit".equals(method)) {
                    messagesForwarded.incrementAndGet();
                    log.info("[{}] ← share submitted (total: {})", 
                        minerId, messagesForwarded.get());
                }
                
                // Forward to upstream
                forwardMessage(jsonMessage, upstreamChannel);
                
            } else {
                // From upstream
                if ("mining.notify".equals(method)) {
                    log.debug("[{}] → mining.notify (new job)", minerId);
                } else if ("mining.set_difficulty".equals(method)) {
                    JsonNode params = json.get("params");
                    log.info("[{}] → difficulty set: {}", minerId, params.get(0));
                }
                
                // Forward to miner
                forwardMessage(jsonMessage, minerChannel);
            }
            
        } catch (Exception e) {
            log.error("[{}] Failed to parse JSON: {}", minerId, e.getMessage());
        }
    }

    private void forwardMessage(String message, AsynchronousSocketChannel targetChannel) {
        if (!active) return;
        
        try {
            byte[] data = (message + "\n").getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.wrap(data);
            
            targetChannel.write(buffer, null, new CompletionHandler<Integer, Void>() {
                @Override
                public void completed(Integer bytesWritten, Void attachment) {
                    // Message sent successfully
                }

                @Override
                public void failed(Throwable exc, Void attachment) {
                    log.error("[{}] Failed to forward message: {}", minerId, exc.getMessage());
                    close();
                }
            });
            
        } catch (Exception e) {
            log.error("[{}] Error forwarding message: {}", minerId, e.getMessage());
        }
    }

    public void close() {
        if (!active) return;
        active = false;
        
        log.info("[{}] ✗ Closing connection (Recv: {}, Fwd: {})", 
            minerId, messagesReceived.get(), messagesForwarded.get());
        
        try {
            if (minerChannel.isOpen()) minerChannel.close();
        } catch (IOException ignored) {}
        
        try {
            if (upstreamChannel.isOpen()) upstreamChannel.close();
        } catch (IOException ignored) {}
    }
}
