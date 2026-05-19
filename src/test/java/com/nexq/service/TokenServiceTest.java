package com.nexq.service;

import com.nexq.dto.TokenResponse;
import com.nexq.exception.QueueOperationException;
import com.nexq.model.*;
import com.nexq.repository.TokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock private TokenRepository tokenRepository;
    @Mock private QueueService queueService;
    @Mock private NotificationService notificationService;
    @Mock private QueueStatusService queueStatusService;

    @InjectMocks private TokenService tokenService;

    private Queue mockQueue;
    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).name("Test User")
                .email("test@example.com").role(UserRole.USER).build();

        mockQueue = Queue.builder().id(1L).name("Test Queue")
                .isActive(true).createdBy(mockUser).build();
    }

    @Test
    void generateToken_ShouldReturnTokenWithNextNumber() {
        when(queueService.findQueueById(1L)).thenReturn(mockQueue);
        when(tokenRepository.findByUserAndQueueAndStatusIn(any(), any(), any())).thenReturn(Optional.empty());
        when(tokenRepository.findMaxTokenNumberByQueue(mockQueue)).thenReturn(5);

        Token savedToken = Token.builder().id(1L).tokenNumber(6)
                .status(TokenStatus.WAITING).queue(mockQueue).user(mockUser)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(30)).build();
        when(tokenRepository.save(any(Token.class))).thenReturn(savedToken);
        when(tokenRepository.countAheadInQueue(any(), anyInt())).thenReturn(5L);

        TokenResponse response = tokenService.generateToken(1L, mockUser);

        assertThat(response.getTokenNumber()).isEqualTo(6);
        assertThat(response.getStatus()).isEqualTo(TokenStatus.WAITING);
        verify(notificationService, times(1)).sendTokenGeneratedNotification(any());
        verify(queueStatusService, times(1)).broadcastQueueUpdate(1L);
    }

    @Test
    void generateToken_ShouldThrow_WhenQueueInactive() {
        mockQueue.setIsActive(false);
        when(queueService.findQueueById(1L)).thenReturn(mockQueue);

        assertThatThrownBy(() -> tokenService.generateToken(1L, mockUser))
                .isInstanceOf(QueueOperationException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void generateToken_ShouldThrow_WhenUserAlreadyHasActiveToken() {
        when(queueService.findQueueById(1L)).thenReturn(mockQueue);
        Token existingToken = Token.builder().id(1L).tokenNumber(3)
                .status(TokenStatus.WAITING).queue(mockQueue).user(mockUser).build();
        when(tokenRepository.findByUserAndQueueAndStatusIn(any(), any(), any()))
                .thenReturn(Optional.of(existingToken));

        assertThatThrownBy(() -> tokenService.generateToken(1L, mockUser))
                .isInstanceOf(QueueOperationException.class)
                .hasMessageContaining("already have an active token");
    }

    @Test
    void serveNextToken_ShouldChangeStatusToServing() {
        when(queueService.findQueueById(1L)).thenReturn(mockQueue);
        Token waitingToken = Token.builder().id(1L).tokenNumber(1)
                .status(TokenStatus.WAITING).queue(mockQueue).user(mockUser)
                .issuedAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusMinutes(30)).build();
        when(tokenRepository.findFirstByQueueAndStatusOrderByTokenNumberAsc(mockQueue, TokenStatus.WAITING))
                .thenReturn(Optional.of(waitingToken));
        when(tokenRepository.findByQueueAndTokenNumber(any(), anyInt())).thenReturn(Optional.empty());

        Token servingToken = Token.builder().id(1L).tokenNumber(1)
                .status(TokenStatus.SERVING).queue(mockQueue).user(mockUser)
                .issuedAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusMinutes(30))
                .servedAt(LocalDateTime.now()).build();
        when(tokenRepository.save(any())).thenReturn(servingToken);

        TokenResponse response = tokenService.serveNextToken(1L);

        assertThat(response.getStatus()).isEqualTo(TokenStatus.SERVING);
    }

    @Test
    void serveNextToken_ShouldThrow_WhenNoWaitingTokens() {
        when(queueService.findQueueById(1L)).thenReturn(mockQueue);
        when(tokenRepository.findFirstByQueueAndStatusOrderByTokenNumberAsc(mockQueue, TokenStatus.WAITING))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.serveNextToken(1L))
                .isInstanceOf(QueueOperationException.class)
                .hasMessageContaining("No waiting tokens");
    }

    @Test
    void completeToken_ShouldThrow_WhenNotServing() {
        Token waitingToken = Token.builder().id(1L).tokenNumber(1)
                .status(TokenStatus.WAITING).queue(mockQueue).user(mockUser)
                .issuedAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusMinutes(30)).build();
        when(tokenRepository.findById(1L)).thenReturn(Optional.of(waitingToken));

        assertThatThrownBy(() -> tokenService.completeToken(1L))
                .isInstanceOf(QueueOperationException.class)
                .hasMessageContaining("not currently being served");
    }
}
