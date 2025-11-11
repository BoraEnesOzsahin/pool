package com.example.miner.controller;

import com.example.miner.service.WorkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/workers")
@Tag(name = "Worker Management", description = "APIs for creating and managing AntPool workers")
public class WorkerController {

    private final WorkerService workerService;

    @Value("${app.workerNamePrefix}")
    private String defaultPrefix;

    public WorkerController(WorkerService workerService) {
        this.workerService = workerService;
    }

    /**
     * Create a new worker with full customization options
     * POST /api/workers
     * Body: { 
     *   "subaccount": "myAntpoolAccount",  // Optional - your AntPool subaccount
     *   "workerName": "mygpu01",           // Optional - custom worker name (or use prefix for auto-generated)
     *   "prefix": "w",                     // Optional - prefix for auto-generated name (ignored if workerName provided)
     *   "password": "MySecurePass123!"     // Optional - custom password (or leave empty for random)
     * }
     * Response: { "worker": "myAntpoolAccount.mygpu01", "password": "MySecurePass123!" }
     */
    @PostMapping
    @Operation(
        summary = "Create a new worker",
        description = "Create a worker with full control: specify subaccount, worker name, and password. All fields are optional - defaults will be used if not provided."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Worker created successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"worker\":\"myAntpoolAccount.mygpu01\",\"password\":\"MySecurePass123!\"}"
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid worker name format or password too weak")
    })
    public ResponseEntity<Map<String, String>> createWorker(
            @RequestBody(required = false) Map<String, String> request) {
        
        String subaccount = null;
        String workerName = null;
        String prefix = defaultPrefix;
        String customPassword = null;
        
        if (request != null) {
            subaccount = request.get("subaccount");
            workerName = request.get("workerName");
            customPassword = request.get("password");
            
            if (request.containsKey("prefix")) {
                prefix = request.get("prefix");
            }
        }
        
        WorkerService.WorkerCreationResult result = workerService.create(
            subaccount, 
            workerName, 
            prefix, 
            customPassword
        );
        
        return ResponseEntity.ok(Map.of(
            "worker", result.getWorkerName(),
            "password", result.getPassword()
        ));
    }
}
