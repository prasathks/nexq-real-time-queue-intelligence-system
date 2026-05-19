package com.nexq.controller;

import com.nexq.dto.QueueRequest;
import com.nexq.dto.QueueResponse;
import com.nexq.model.User;
import com.nexq.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.nexq.service.QRService;

@RestController
@RequestMapping("/api/queues")
@RequiredArgsConstructor
@Tag(name = "Queues", description = "Queue management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class QueueController {

    private final QueueService queueService;
    private final QRService qrService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Create a new queue")
    public ResponseEntity<QueueResponse> createQueue(
            @Valid @RequestBody QueueRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(queueService.createQueue(request, currentUser));
    }

    @GetMapping
    @Operation(summary = "Get all active queues")
    public ResponseEntity<List<QueueResponse>> getAllActiveQueues() {
        return ResponseEntity.ok(queueService.getAllActiveQueues());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all queues including inactive (ADMIN only)")
    public ResponseEntity<List<QueueResponse>> getAllQueues() {
        return ResponseEntity.ok(queueService.getAllQueues());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get queue details by ID")
    public ResponseEntity<QueueResponse> getQueue(@PathVariable Long id) {
        return ResponseEntity.ok(queueService.getQueueById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Update queue details")
    public ResponseEntity<QueueResponse> updateQueue(
            @PathVariable Long id,
            @Valid @RequestBody QueueRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(queueService.updateQueue(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Deactivate a queue")
    public ResponseEntity<Void> deactivateQueue(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        queueService.deactivateQueue(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Get QR code to join queue")
    public ResponseEntity<byte[]> getQueueQRCode(@PathVariable Long id) {
        try {
            // Verify queue exists
            queueService.getQueueById(id);
            // In a real app this would be a full domain, using localhost for demo
            String joinUrl = "http://localhost:8080/user-dashboard.html?joinQueue=" + id;
            byte[] image = qrService.generateQRCodeImage(joinUrl, 250, 250);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"queue-" + id + "-qr.png\"")
                    .body(image);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
