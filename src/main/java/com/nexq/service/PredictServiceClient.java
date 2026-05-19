package com.nexq.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class PredictServiceClient {

    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    public int predictWaitTime(int waitingCount, int priorityWeight, Long queueId) {
        try {
            String url = aiServiceUrl + "/predict";
            PredictionRequest request = new PredictionRequest(waitingCount, priorityWeight, queueId);
            ResponseEntity<PredictionResponse> response = restTemplate.postForEntity(url, request, PredictionResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getEstimatedWaitMinutes();
            }
        } catch (Exception e) {
            log.error("Failed to predict wait time: {}", e.getMessage());
        }
        // Fallback: 5 mins per person
        return waitingCount * 5;
    }

    @Data
    static class PredictionRequest {
        private int waitingCount;
        private int priorityWeight;
        private Long queueId;

        public PredictionRequest(int waitingCount, int priorityWeight, Long queueId) {
            this.waitingCount = waitingCount;
            this.priorityWeight = priorityWeight;
            this.queueId = queueId;
        }
    }

    @Data
    static class PredictionResponse {
        private int estimatedWaitMinutes;
    }
}
