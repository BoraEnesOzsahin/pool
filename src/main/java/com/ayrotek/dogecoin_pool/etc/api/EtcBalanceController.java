package com.ayrotek.dogecoin_pool.etc.api;

import com.ayrotek.dogecoin_pool.etc.client.TatumEtcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/etc")
public class EtcBalanceController {

    private static final Logger log = LoggerFactory.getLogger(EtcBalanceController.class);

    private final TatumEtcClient tatumEtcClient;

    public EtcBalanceController(TatumEtcClient tatumEtcClient) {
        this.tatumEtcClient = tatumEtcClient;
    }

    /**
     * Get ETC testnet balance for a given address.
     * 
     * @param address ETC address (0x...)
     * @return Balance information in Wei
     */
    @GetMapping("/balance/{address}")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable String address) {
        log.info("Getting ETC balance for address: {}", address);
        
        try {
            Map<String, Object> balance = tatumEtcClient.getAddressBalance(address);
            return ResponseEntity.ok(balance);
        } catch (IllegalArgumentException e) {
            log.error("Invalid address format: {}", address, e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching ETC balance for {}", address, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch balance: " + e.getMessage()));
        }
    }

    /**
     * Get current gas price for ETC testnet.
     */
    @GetMapping("/gas-price")
    public ResponseEntity<Map<String, Object>> getGasPrice() {
        log.info("Getting ETC gas price");
        
        try {
            Map<String, Object> gasPrice = tatumEtcClient.getGasPrice();
            return ResponseEntity.ok(gasPrice);
        } catch (Exception e) {
            log.error("Error fetching gas price", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch gas price: " + e.getMessage()));
        }
    }

    /**
     * Get hot wallet configuration info (without sensitive data).
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        var props = tatumEtcClient.getProperties();
        return ResponseEntity.ok(Map.of(
            "baseUrl", props.getBaseUrl(),
            "hotAddress", props.getHotAddress() != null ? props.getHotAddress() : "Not configured",
            "coldAddress", props.getColdAddress() != null ? props.getColdAddress() : "Not configured",
            "minSweepEtc", props.getMinSweepEtc(),
            "reserveEtc", props.getReserveEtc()
        ));
    }
}
