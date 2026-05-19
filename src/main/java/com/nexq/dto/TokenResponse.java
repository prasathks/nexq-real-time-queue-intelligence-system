package com.nexq.dto;

import com.nexq.model.TokenStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private Long id;
    private Integer tokenNumber;
    private TokenStatus status;
    private Integer priorityWeight;
    private Long queueId;
    private String queueName;
    private String userName;
    private String userEmail;
    private LocalDateTime issuedAt;
    private LocalDateTime servedAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;
    private long positionInQueue;
    private long estimatedWaitMinutes;
}
