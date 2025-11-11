package com.example.miner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/miner-config")
@Tag(name = "Miner Configuration", description = "APIs for generating mining software configurations")
public class MinerConfigController {

    /**
     * Generate SRBMiner configuration
     * GET /api/miner-config/srbminer?worker=subaccount.w-abc123&pass=plaintextPassword
     * Response: JSON config for SRBMiner
     */
    @GetMapping("/srbminer")
    @Operation(
        summary = "Get SRBMiner configuration",
        description = "Returns ready-to-use JSON configuration for SRBMiner software with RandomX algorithm"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Configuration generated successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"ALGO\":\"randomx\",\"POOL\":\"stratum+tcp://xmr.antpool.com:9005\",\"WALLET\":\"yourAntpoolSubaccount.w-1a2b3c4d\",\"PASSWORD\":\"Xy9@bK#mN2pQ$rTv\",\"CPU_THREADS\":\"0\",\"DISABLE_GPU\":\"true\"}"
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid worker name format")
    })
    public ResponseEntity<Map<String, String>> getSrbMinerConfig(
            @Parameter(description = "Worker name in format: subaccount.workername", example = "yourAntpoolSubaccount.w-1a2b3c4d")
            @RequestParam String worker,
            @Parameter(description = "Worker password", example = "Xy9@bK#mN2pQ$rTv")
            @RequestParam String pass) {
        
        // Validate worker name format
        if (!worker.matches("^[A-Za-z0-9_-]{1,64}\\.[A-Za-z0-9_-]{1,64}$")) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid worker name format"));
        }
        
        // Build config (using LinkedHashMap to preserve order)
        Map<String, String> config = new LinkedHashMap<>();
        config.put("ALGO", "randomx");
        config.put("POOL", "stratum+tcp://xmr.antpool.com:9005");
        config.put("WALLET", worker);
        config.put("PASSWORD", pass);
        config.put("CPU_THREADS", "0");  // 0 = auto-detect
        config.put("DISABLE_GPU", "true");
        
        return ResponseEntity.ok(config);
    }
}
