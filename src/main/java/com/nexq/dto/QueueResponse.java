package com.nexq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueResponse {
    private Long id;
    private String name;
    private String description;
    private String location;
    private Integer maxCapacity;
    private Long branchId;
    private String branchName;
    private Boolean isActive;
    private String createdByName;
    private Long createdById;
    private LocalDateTime createdAt;
    private long waitingCount;
    private long servingCount;
    private long completedToday;
}
