package com.mediguide.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Public response DTO returning the outcome of a batch availability pre-scheduling request.
 * Documents both successfully created slots and rejected slots with specific reasons.
 */
public class BatchAvailabilityResponse {

    private int totalRequested;
    private int totalCreated;
    private int totalFailed;
    private List<AvailabilityResponse> successfulSlots = new ArrayList<>();
    private List<BatchSlotError> rejectedSlots = new ArrayList<>();

    public BatchAvailabilityResponse() {
    }

    public BatchAvailabilityResponse(int totalRequested, int totalCreated, int totalFailed,
                                   List<AvailabilityResponse> successfulSlots,
                                   List<BatchSlotError> rejectedSlots) {
        this.totalRequested = totalRequested;
        this.totalCreated = totalCreated;
        this.totalFailed = totalFailed;
        this.successfulSlots = successfulSlots != null ? successfulSlots : new ArrayList<>();
        this.rejectedSlots = rejectedSlots != null ? rejectedSlots : new ArrayList<>();
    }

    public int getTotalRequested() {
        return totalRequested;
    }

    public void setTotalRequested(int totalRequested) {
        this.totalRequested = totalRequested;
    }

    public int getTotalCreated() {
        return totalCreated;
    }

    public void setTotalCreated(int totalCreated) {
        this.totalCreated = totalCreated;
    }

    public int getTotalFailed() {
        return totalFailed;
    }

    public void setTotalFailed(int totalFailed) {
        this.totalFailed = totalFailed;
    }

    public List<AvailabilityResponse> getSuccessfulSlots() {
        return successfulSlots;
    }

    public void setSuccessfulSlots(List<AvailabilityResponse> successfulSlots) {
        this.successfulSlots = successfulSlots;
    }

    public List<BatchSlotError> getRejectedSlots() {
        return rejectedSlots;
    }

    public void setRejectedSlots(List<BatchSlotError> rejectedSlots) {
        this.rejectedSlots = rejectedSlots;
    }
}
