package com.nexq.repository;

import com.nexq.model.Queue;
import com.nexq.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QueueRepository extends JpaRepository<Queue, Long> {
    List<Queue> findByIsActiveTrue();
    List<Queue> findByCreatedBy(User createdBy);
    List<Queue> findByIsActiveTrueOrderByCreatedAtDesc();
}
