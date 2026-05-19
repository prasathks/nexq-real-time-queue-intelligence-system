package com.nexq.scheduler;

import com.nexq.model.*;
import com.nexq.repository.TokenRepository;
import com.nexq.service.NotificationService;
import com.nexq.service.QueueStatusService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenExpirySchedulerTest {

    @Mock private TokenRepository tokenRepository;
    @Mock private NotificationService notificationService;
    @Mock private QueueStatusService queueStatusService;

    @InjectMocks private TokenExpiryScheduler scheduler;

    @Test
    void expireStaleTokens_ShouldExpireAndNotify_WhenExpiredTokensExist() {
        User user = User.builder().id(1L).name("User").email("u@test.com").role(UserRole.USER).build();
        Queue queue = Queue.builder().id(1L).name("Q1").isActive(true).createdBy(user).build();

        Token expiredToken = Token.builder()
                .id(1L).tokenNumber(1).status(TokenStatus.WAITING)
                .queue(queue).user(user)
                .issuedAt(LocalDateTime.now().minusHours(1))
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(tokenRepository.findExpiredTokens(any())).thenReturn(List.of(expiredToken));
        when(tokenRepository.save(any())).thenReturn(expiredToken);

        scheduler.expireStaleTokens();

        assertThat(expiredToken.getStatus()).isEqualTo(TokenStatus.EXPIRED);
        verify(notificationService, times(1)).sendTokenExpiredNotification(expiredToken);
        verify(queueStatusService, times(1)).broadcastQueueUpdate(1L);
    }

    @Test
    void expireStaleTokens_ShouldDoNothing_WhenNoExpiredTokens() {
        when(tokenRepository.findExpiredTokens(any())).thenReturn(List.of());

        scheduler.expireStaleTokens();

        verify(tokenRepository, never()).save(any());
        verify(notificationService, never()).sendTokenExpiredNotification(any());
    }
}
