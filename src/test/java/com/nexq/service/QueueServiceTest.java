package com.nexq.service;

import com.nexq.dto.QueueRequest;
import com.nexq.dto.QueueResponse;
import com.nexq.exception.ResourceNotFoundException;
import com.nexq.exception.UnauthorizedException;
import com.nexq.model.Queue;
import com.nexq.model.User;
import com.nexq.model.UserRole;
import com.nexq.repository.QueueRepository;
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
class QueueServiceTest {

    @Mock private QueueRepository queueRepository;
    @Mock private TokenRepository tokenRepository;

    @InjectMocks private QueueService queueService;

    private User adminUser;
    private User staffUser;
    private User otherUser;
    private Queue testQueue;

    @BeforeEach
    void setUp() {
        adminUser = User.builder().id(1L).name("Admin").email("admin@nexq.com").role(UserRole.ADMIN).build();
        staffUser = User.builder().id(2L).name("Staff").email("staff@nexq.com").role(UserRole.STAFF).build();
        otherUser = User.builder().id(3L).name("Other").email("other@nexq.com").role(UserRole.STAFF).build();

        testQueue = Queue.builder().id(1L).name("Test Queue")
                .description("A test queue").location("Floor 1")
                .isActive(true).createdBy(staffUser)
                .createdAt(LocalDateTime.now()).build();
    }

    @Test
    void createQueue_ShouldReturnQueueResponse() {
        QueueRequest request = new QueueRequest();
        request.setName("New Queue");
        request.setDescription("Desc");
        request.setLocation("Counter A");

        when(queueRepository.save(any(Queue.class))).thenReturn(testQueue);

        QueueResponse response = queueService.createQueue(request, staffUser);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Test Queue");
        verify(queueRepository, times(1)).save(any(Queue.class));
    }

    @Test
    void getAllActiveQueues_ShouldReturnOnlyActiveQueues() {
        when(queueRepository.findByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(testQueue));

        List<QueueResponse> queues = queueService.getAllActiveQueues();

        assertThat(queues).hasSize(1);
        assertThat(queues.get(0).getIsActive()).isTrue();
    }

    @Test
    void getQueueById_ShouldThrow_WhenNotFound() {
        when(queueRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queueService.getQueueById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void deactivateQueue_ShouldSetInactive_WhenOwner() {
        when(queueRepository.findById(1L)).thenReturn(Optional.of(testQueue));
        when(queueRepository.save(any())).thenReturn(testQueue);

        queueService.deactivateQueue(1L, staffUser);

        assertThat(testQueue.getIsActive()).isFalse();
        verify(queueRepository, times(1)).save(testQueue);
    }

    @Test
    void deactivateQueue_ShouldSucceed_WhenAdmin() {
        when(queueRepository.findById(1L)).thenReturn(Optional.of(testQueue));
        when(queueRepository.save(any())).thenReturn(testQueue);

        queueService.deactivateQueue(1L, adminUser);

        assertThat(testQueue.getIsActive()).isFalse();
    }

    @Test
    void deactivateQueue_ShouldThrow_WhenNotOwnerAndNotAdmin() {
        when(queueRepository.findById(1L)).thenReturn(Optional.of(testQueue));

        assertThatThrownBy(() -> queueService.deactivateQueue(1L, otherUser))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void updateQueue_ShouldUpdateFields_WhenOwner() {
        QueueRequest update = new QueueRequest();
        update.setName("Updated Name");
        update.setLocation("Counter B");

        when(queueRepository.findById(1L)).thenReturn(Optional.of(testQueue));
        when(queueRepository.save(any())).thenReturn(testQueue);

        QueueResponse response = queueService.updateQueue(1L, update, staffUser);

        assertThat(testQueue.getName()).isEqualTo("Updated Name");
    }
}
