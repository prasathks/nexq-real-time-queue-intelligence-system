package com.nexq.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BranchResponse {
    private Long id;
    private String name;
    private String location;
    private Boolean isActive;
    private int activeQueuesCount;
}
