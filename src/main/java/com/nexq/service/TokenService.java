package com.nexq.service;

import com.nexq.dto.TokenRequest;
import com.nexq.dto.TokenResponse;
import com.nexq.exception.QueueOperationException;
import com.nexq.exception.ResourceNotFoundException;
import com.nexq.model.*;
import com.nexq.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.RedisTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final TokenRepository tokenRepository;
    private final QueueService queueService;
    private final NotificationService notificationService;
    private final QueueStatusService queueStatusService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PredictServiceClient predictServiceClient;

    @Value("${nexq.token.expiry.minutes:30}")
    private int tokenExpiryMinutes;

    private static final int AVG_SERVE_TIME_MINUTES = 5;

    // Generate token and lock row to prevent duplicates when traffic spikes
    @Transactional
    public TokenResponse generateToken(Long queueId, User user, TokenRequest request) {
        Queue queue = queueService.findQueueById(queueId);

        if (!queue.getIsActive()) {
            throw new QueueOperationException("Queue is not active");
        }

        if (queue.getMaxCapacity() != null && queue.getMaxCapacity() > 0) {
            long currentWaiting = tokenRepository.countByQueueAndStatus(queue, TokenStatus.WAITING);
            if (currentWaiting >= queue.getMaxCapacity()) {
                throw new QueueOperationException("Queue is full. Maximum capacity is " + queue.getMaxCapacity());
            }
        }

        // Check if user already has an active token in this queue
        List<TokenStatus> activeStatuses = List.of(TokenStatus.WAITING, TokenStatus.SERVING);
        tokenRepository.findByUserAndQueueAndStatusIn(user, queue, activeStatuses)
                .ifPresent(t -> {
                    throw new QueueOperationException(
                            "You already have an active token #" + t.getTokenNumber() + " in this queue");
                });

        // Generate token using ultra-fast Redis INCR instead of MySQL locks
        String redisKey = "queue:" + queueId + ":counter";
        Long generatedNumber = redisTemplate.opsForValue().increment(redisKey);
        int nextTokenNumber = generatedNumber != null ? generatedNumber.intValue() : 1;

        int priorityWeight = (request != null && request.getPriorityWeight() != null) ? request.getPriorityWeight() : 0;
        int waitingCount = (int) tokenRepository.countByQueueAndStatus(queue, TokenStatus.WAITING);
        int predictedWait = predictServiceClient.predictWaitTime(waitingCount, priorityWeight, queueId);

        Token token = Token.builder()
                .tokenNumber(nextTokenNumber)
                .priorityWeight(priorityWeight)
                .estimatedWaitMinutes(predictedWait)
                .status(TokenStatus.WAITING)
                .queue(queue)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(tokenExpiryMinutes))
                .build();

        Token saved = tokenRepository.save(token);
        log.info("Token #{} generated for user {} in queue {}", nextTokenNumber, user.getEmail(), queue.getName());

        // Notify user async
        notificationService.sendTokenGeneratedNotification(saved);

        // Broadcast SSE update
        queueStatusService.broadcastQueueUpdate(queueId);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public TokenResponse getTokenStatus(Long tokenId) {
        Token token = findTokenById(tokenId);
        return mapToResponse(token);
    }

    @Transactional(readOnly = true)
    public List<TokenResponse> getUserTokens(User user) {
        return tokenRepository.findByUserOrderByIssuedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TokenResponse> getQueueTokensByStatus(Long queueId, TokenStatus status) {
        Queue queue = queueService.findQueueById(queueId);
        return tokenRepository.findByQueueAndStatusOrderByPriorityWeightDescTokenNumberAsc(queue, status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Move the next person in line to SERVING state
    @Transactional
    public TokenResponse serveNextToken(Long queueId) {
        Queue queue = queueService.findQueueById(queueId);

        Token token = tokenRepository.findFirstByQueueAndStatusOrderByPriorityWeightDescTokenNumberAsc(queue, TokenStatus.WAITING)
                .orElseThrow(() -> new QueueOperationException("No waiting tokens in this queue"));

        token.setStatus(TokenStatus.SERVING);
        token.setServedAt(LocalDateTime.now());
        Token saved = tokenRepository.save(token);

        // Notify user 2 positions ahead
        notifyApproachingUser(queue, token.getTokenNumber());

        queueStatusService.broadcastQueueUpdate(queueId);
        log.info("Token #{} now SERVING in queue {}", token.getTokenNumber(), queue.getName());

        return mapToResponse(saved);
    }

    // Mark token as done
    @Transactional
    public TokenResponse completeToken(Long tokenId) {
        Token token = findTokenById(tokenId);

        if (token.getStatus() != TokenStatus.SERVING) {
            throw new QueueOperationException("Token is not currently being served");
        }

        token.setStatus(TokenStatus.COMPLETED);
        token.setCompletedAt(LocalDateTime.now());
        Token saved = tokenRepository.save(token);

        queueStatusService.broadcastQueueUpdate(token.getQueue().getId());
        log.info("Token #{} COMPLETED", token.getTokenNumber());

        return mapToResponse(saved);
    }

    // Skip no-shows
    @Transactional
    public TokenResponse skipToken(Long tokenId) {
        Token token = findTokenById(tokenId);

        if (token.getStatus() != TokenStatus.SERVING && token.getStatus() != TokenStatus.WAITING) {
            throw new QueueOperationException("Token cannot be skipped in its current state");
        }

        token.setStatus(TokenStatus.SKIPPED);
        Token saved = tokenRepository.save(token);

        queueStatusService.broadcastQueueUpdate(token.getQueue().getId());
        log.info("Token #{} SKIPPED", token.getTokenNumber());

        return mapToResponse(saved);
    }

    private void notifyApproachingUser(Queue queue, int currentTokenNumber) {
        int approachingTokenNumber = currentTokenNumber + 2;
        tokenRepository.findByQueueAndTokenNumber(queue, approachingTokenNumber)
                .ifPresent(notificationService::sendTurnApproachingNotification);
    }

    public Token findTokenById(Long id) {
        return tokenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found with id: " + id));
    }

    public TokenResponse mapToResponse(Token token) {
        Queue queue = token.getQueue();
        long position = 0;
        long estimatedWait = 0;

        if (token.getStatus() == TokenStatus.WAITING) {
            position = tokenRepository.countAheadInQueue(queue, token.getTokenNumber()) + 1;
            estimatedWait = token.getEstimatedWaitMinutes() != null ? token.getEstimatedWaitMinutes() : (position * AVG_SERVE_TIME_MINUTES);
        }

        return TokenResponse.builder()
                .id(token.getId())
                .tokenNumber(token.getTokenNumber())
                .status(token.getStatus())
                .priorityWeight(token.getPriorityWeight())
                .queueId(queue.getId())
                .queueName(queue.getName())
                .userName(token.getUser().getName())
                .userEmail(token.getUser().getEmail())
                .issuedAt(token.getIssuedAt())
                .servedAt(token.getServedAt())
                .completedAt(token.getCompletedAt())
                .expiresAt(token.getExpiresAt())
                .positionInQueue(position)
                .estimatedWaitMinutes(estimatedWait)
                .build();
    }
}
