package com.nexq.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class QueueStatusService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastQueueUpdate(Long queueId) {
        String destination = "/topic/queue/" + queueId;
        String payload = "{\"queueId\":" + queueId + ",\"timestamp\":\"" + LocalDateTime.now() + "\"}";
        
        messagingTemplate.convertAndSend(destination, payload);
        log.debug("WebSocket broadcast sent to {}", destination);
    }
}
