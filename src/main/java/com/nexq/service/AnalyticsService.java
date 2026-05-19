package com.nexq.service;

import com.nexq.model.Queue;
import com.nexq.repository.TokenRepository;
import com.nexq.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TokenRepository tokenRepository;
    private final QueueService queueService;

    @Transactional(readOnly = true)
    public Map<String, Object> getWaitTimeByHour(Long queueId) {
        Queue queue = queueService.findQueueById(queueId);
        List<Object[]> results = tokenRepository.findAvgWaitTimeByHour(queue);

        List<Integer> hours = new ArrayList<>();
        List<Double> avgWaits = new ArrayList<>();

        for (Object[] row : results) {
            hours.add(((Number) row[0]).intValue());
            avgWaits.add(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("queueId", queueId);
        response.put("queueName", queue.getName());
        response.put("hours", hours);
        response.put("avgWaitMinutes", avgWaits);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPeakHours(Long queueId) {
        Queue queue = queueService.findQueueById(queueId);
        List<Object[]> results = tokenRepository.findPeakHours(queue);

        List<Map<String, Object>> peakData = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("hour", ((Number) row[0]).intValue());
            entry.put("count", ((Number) row[1]).longValue());
            peakData.add(entry);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("queueId", queueId);
        response.put("queueName", queue.getName());
        response.put("peakHours", peakData);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDailyStats(Long queueId) {
        Queue queue = queueService.findQueueById(queueId);
        List<Object[]> results = tokenRepository.findDailyStats(queue);

        List<Map<String, Object>> dailyData = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", row[0].toString());
            entry.put("total", ((Number) row[1]).longValue());
            entry.put("served", ((Number) row[2]).longValue());
            dailyData.add(entry);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("queueId", queueId);
        response.put("queueName", queue.getName());
        response.put("dailyStats", dailyData);
        return response;
    }
}
