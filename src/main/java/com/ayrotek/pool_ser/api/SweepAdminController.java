package com.ayrotek.pool_ser.api;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ayrotek.pool_ser.api.dto.SweepRecordDto;
import com.ayrotek.pool_ser.api.mapper.SweepRecordMapper;
import com.ayrotek.pool_ser.entity.SweepRecord;
import com.ayrotek.pool_ser.entity.SweepStatus;
import com.ayrotek.pool_ser.repository.SweepRecordRepository;

@RestController
@RequestMapping("/api/sweeps")
public class SweepAdminController {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final SweepRecordRepository sweepRecordRepository;

    public SweepAdminController(SweepRecordRepository sweepRecordRepository) {
        this.sweepRecordRepository = sweepRecordRepository;
    }

    @GetMapping("/recent")
    public List<SweepRecordDto> recentSweeps(@RequestParam(name = "limit", required = false) Integer limit) {
        int limitValue = sanitizeLimit(limit);
        return sweepRecordRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .limit(limitValue)
                .map(SweepRecordMapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public SweepRecordDto sweepById(@PathVariable("id") Long id) {
        SweepRecord record = sweepRecordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sweep not found"));
        return SweepRecordMapper.toDto(record);
    }

    @GetMapping("/byStatus")
    public List<SweepRecordDto> sweepsByStatus(@RequestParam("status") String status) {
        SweepStatus sweepStatus = parseStatus(status);
        return sweepRecordRepository.findTop50ByStatusOrderByCreatedAtDesc(sweepStatus).stream()
                .map(SweepRecordMapper::toDto)
                .toList();
    }

    private int sanitizeLimit(Integer requestedLimit) {
        if (requestedLimit == null) {
            return DEFAULT_LIMIT;
        }
        int value = Math.max(1, requestedLimit);
        return Math.min(value, MAX_LIMIT);
    }

    private SweepStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status query parameter is required");
        }
        try {
            return SweepStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown sweep status: " + rawStatus);
        }
    }
}
