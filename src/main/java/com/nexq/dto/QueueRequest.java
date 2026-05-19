package com.nexq.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QueueRequest {
    @NotBlank(message = "Queue name is required")
    private String name;

    private String description;
    private String location;
    private Integer maxCapacity;
    
    private Long branchId;
}
