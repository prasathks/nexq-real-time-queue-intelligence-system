package com.nexq.controller;

import com.nexq.dto.TokenRequest;
import com.nexq.dto.TokenResponse;
import com.nexq.model.TokenStatus;
import com.nexq.model.User;
import com.nexq.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Tokens", description = "Token generation and queue management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TokenController {

    private final TokenService tokenService;

    @PostMapping("/queues/{queueId}/tokens")
    @Operation(summary = "Join a queue and receive a token")
    public ResponseEntity<TokenResponse> joinQueue(
            @PathVariable Long queueId,
            @RequestBody(required = false) TokenRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tokenService.generateToken(queueId, currentUser, request));
    }

    @GetMapping("/tokens/{tokenId}/status")
    @Operation(summary = "Check token status and queue position")
    public ResponseEntity<TokenResponse> getTokenStatus(@PathVariable Long tokenId) {
        return ResponseEntity.ok(tokenService.getTokenStatus(tokenId));
    }

    @GetMapping("/tokens/my")
    @Operation(summary = "Get all tokens for current user")
    public ResponseEntity<List<TokenResponse>> getMyTokens(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(tokenService.getUserTokens(currentUser));
    }

    @GetMapping("/queues/{queueId}/tokens")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get all WAITING tokens in a queue (Staff/Admin)")
    public ResponseEntity<List<TokenResponse>> getQueueTokens(
            @PathVariable Long queueId,
            @RequestParam(defaultValue = "WAITING") String status) {
        TokenStatus tokenStatus = TokenStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(tokenService.getQueueTokensByStatus(queueId, tokenStatus));
    }

    @PutMapping("/queues/{queueId}/tokens/serve-next")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Serve the next waiting token in queue")
    public ResponseEntity<TokenResponse> serveNext(@PathVariable Long queueId) {
        return ResponseEntity.ok(tokenService.serveNextToken(queueId));
    }

    @PutMapping("/tokens/{tokenId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Mark a token as completed")
    public ResponseEntity<TokenResponse> completeToken(@PathVariable Long tokenId) {
        return ResponseEntity.ok(tokenService.completeToken(tokenId));
    }

    @PutMapping("/tokens/{tokenId}/skip")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Skip a token")
    public ResponseEntity<TokenResponse> skipToken(@PathVariable Long tokenId) {
        return ResponseEntity.ok(tokenService.skipToken(tokenId));
    }
}
