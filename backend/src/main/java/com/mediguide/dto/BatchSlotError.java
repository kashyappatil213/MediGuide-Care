package com.mediguide.dto;

import java.time.LocalDateTime;

/**
 * Details of a slot rejected during batch availability pre-scheduling.
 */
public class BatchSlotError {

    private int index;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;

    public BatchSlotError() {
    }

    public BatchSlotError(int index, LocalDateTime startTime, LocalDateTime endTime, String reason) {
        this.index = index;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
