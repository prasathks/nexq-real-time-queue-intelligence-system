package com.nexq.controller;

import com.nexq.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Queue analytics and metrics endpoints")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/queues/{queueId}/wait-time")
    @Operation(summary = "Average wait time by hour of day")
    public ResponseEntity<Map<String, Object>> getWaitTime(@PathVariable Long queueId) {
        return ResponseEntity.ok(analyticsService.getWaitTimeByHour(queueId));
    }

    @GetMapping("/queues/{queueId}/peak-hours")
    @Operation(summary = "Peak traffic hours for a queue")
    public ResponseEntity<Map<String, Object>> getPeakHours(@PathVariable Long queueId) {
        return ResponseEntity.ok(analyticsService.getPeakHours(queueId));
    }

    @GetMapping("/queues/{queueId}/daily-stats")
    @Operation(summary = "Daily token counts (total vs served)")
    public ResponseEntity<Map<String, Object>> getDailyStats(@PathVariable Long queueId) {
        return ResponseEntity.ok(analyticsService.getDailyStats(queueId));
    }
}
