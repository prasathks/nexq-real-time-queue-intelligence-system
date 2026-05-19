package com.nexq.scheduler;

import com.nexq.model.Token;
import com.nexq.model.TokenStatus;
import com.nexq.repository.TokenRepository;
import com.nexq.service.NotificationService;
import com.nexq.service.QueueStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenExpiryScheduler {

    private final TokenRepository tokenRepository;
    private final NotificationService notificationService;
    private final QueueStatusService queueStatusService;

    /**
     * Runs every 60 seconds. Expires tokens past their expiresAt timestamp.
     * Processes in batch to prevent transaction timeouts.
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void expireStaleTokens() {
        LocalDateTime now = LocalDateTime.now();
        List<Token> expiredTokens = tokenRepository.findExpiredTokens(now);

        if (expiredTokens.isEmpty()) {
            log.debug("Token expiry check: no expired tokens found");
            return;
        }

        log.info("Expiring {} stale tokens", expiredTokens.size());

        Set<Long> affectedQueueIds = expiredTokens.stream()
                .map(t -> t.getQueue().getId())
                .collect(Collectors.toSet());

        expiredTokens.forEach(token -> {
            token.setStatus(TokenStatus.EXPIRED);
            tokenRepository.save(token);
            notificationService.sendTokenExpiredNotification(token);
        });

        // Broadcast SSE update for all affected queues
        affectedQueueIds.forEach(queueStatusService::broadcastQueueUpdate);

        log.info("Expired {} tokens across {} queues", expiredTokens.size(), affectedQueueIds.size());
    }
}
