package com.nexq.service;

import com.nexq.dto.BranchRequest;
import com.nexq.dto.BranchResponse;
import com.nexq.exception.ResourceNotFoundException;
import com.nexq.model.Branch;
import com.nexq.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    @Transactional
    public BranchResponse createBranch(BranchRequest request) {
        if (branchRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Branch name already exists");
        }

        Branch branch = Branch.builder()
                .name(request.getName())
                .location(request.getLocation())
                .isActive(true)
                .build();

        return mapToResponse(branchRepository.save(branch));
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> getAllActiveBranches() {
        return branchRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> getAllBranches() {
        return branchRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Branch findBranchById(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id));
    }

    @Transactional
    public void deactivateBranch(Long id) {
        Branch branch = findBranchById(id);
        branch.setIsActive(false);
        branchRepository.save(branch);
    }

    private BranchResponse mapToResponse(Branch branch) {
        int activeQueues = branch.getQueues() != null ? 
            (int) branch.getQueues().stream().filter(q -> q.getIsActive()).count() : 0;

        return BranchResponse.builder()
                .id(branch.getId())
                .name(branch.getName())
                .location(branch.getLocation())
                .isActive(branch.getIsActive())
                .activeQueuesCount(activeQueues)
                .build();
    }
}
