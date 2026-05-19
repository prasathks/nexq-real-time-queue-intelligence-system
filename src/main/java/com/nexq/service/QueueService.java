package com.nexq.service;

import com.nexq.dto.QueueRequest;
import com.nexq.dto.QueueResponse;
import com.nexq.exception.ResourceNotFoundException;
import com.nexq.exception.UnauthorizedException;
import com.nexq.model.Queue;
import com.nexq.model.Branch;
import com.nexq.model.TokenStatus;
import com.nexq.model.User;
import com.nexq.model.UserRole;
import com.nexq.repository.QueueRepository;
import com.nexq.repository.TokenRepository;
import com.nexq.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private final QueueRepository queueRepository;
    private final TokenRepository tokenRepository;
    private final BranchRepository branchRepository;

    @Transactional
    public QueueResponse createQueue(QueueRequest request, User currentUser) {
        Branch branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        }

        Queue queue = Queue.builder()
                .name(request.getName())
                .description(request.getDescription())
                .location(request.getLocation())
                .maxCapacity(request.getMaxCapacity())
                .branch(branch)
                .isActive(true)
                .createdBy(currentUser)
                .build();
        Queue saved = queueRepository.save(queue);
        log.info("Queue created: {} by user {}", saved.getName(), currentUser.getEmail());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<QueueResponse> getAllActiveQueues() {
        return queueRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<QueueResponse> getAllQueues() {
        return queueRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QueueResponse getQueueById(Long id) {
        Queue queue = findQueueById(id);
        return mapToResponse(queue);
    }

    @Transactional
    public QueueResponse updateQueue(Long id, QueueRequest request, User currentUser) {
        Queue queue = findQueueById(id);
        if (!queue.getCreatedBy().getId().equals(currentUser.getId())
                && currentUser.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("You are not authorized to update this queue");
        }
        queue.setName(request.getName());
        if (request.getDescription() != null) queue.setDescription(request.getDescription());
        if (request.getLocation() != null) queue.setLocation(request.getLocation());
        if (request.getMaxCapacity() != null) queue.setMaxCapacity(request.getMaxCapacity());
        if (request.getBranchId() != null) {
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
            queue.setBranch(branch);
        }
        return mapToResponse(queueRepository.save(queue));
    }

    @Transactional
    public void deactivateQueue(Long id, User currentUser) {
        Queue queue = findQueueById(id);
        if (!queue.getCreatedBy().getId().equals(currentUser.getId())
                && currentUser.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("You are not authorized to deactivate this queue");
        }
        queue.setIsActive(false);
        queueRepository.save(queue);
        log.info("Queue deactivated: {} by {}", queue.getName(), currentUser.getEmail());
    }

    public Queue findQueueById(Long id) {
        return queueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Queue not found with id: " + id));
    }

    public QueueResponse mapToResponse(Queue queue) {
        long waitingCount = tokenRepository.countByQueueAndStatus(queue, TokenStatus.WAITING);
        long servingCount = tokenRepository.countByQueueAndStatus(queue, TokenStatus.SERVING);
        long completedCount = tokenRepository.countByQueueAndStatus(queue, TokenStatus.COMPLETED);

        return QueueResponse.builder()
                .id(queue.getId())
                .name(queue.getName())
                .description(queue.getDescription())
                .location(queue.getLocation())
                .maxCapacity(queue.getMaxCapacity())
                .branchId(queue.getBranch() != null ? queue.getBranch().getId() : null)
                .branchName(queue.getBranch() != null ? queue.getBranch().getName() : null)
                .isActive(queue.getIsActive())
                .createdByName(queue.getCreatedBy().getName())
                .createdById(queue.getCreatedBy().getId())
                .createdAt(queue.getCreatedAt())
                .waitingCount(waitingCount)
                .servingCount(servingCount)
                .completedToday(completedCount)
                .build();
    }
}
