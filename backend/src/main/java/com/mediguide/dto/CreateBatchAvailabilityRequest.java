package com.mediguide.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for pre-scheduling multiple doctor availability time slots in a single batch.
 */
public class CreateBatchAvailabilityRequest {

    @NotEmpty(message = "At least one availability slot must be provided")
    @Valid
    private List<CreateAvailabilityRequest> slots = new ArrayList<>();

    /**
     * Optional doctor ID. If provided, the service verifies that it strictly matches
     * the authenticated doctor's identity.
     */
    private Long doctorId;

    public CreateBatchAvailabilityRequest() {
    }

    public CreateBatchAvailabilityRequest(List<CreateAvailabilityRequest> slots) {
        this.slots = slots != null ? slots : new ArrayList<>();
    }

    public CreateBatchAvailabilityRequest(List<CreateAvailabilityRequest> slots, Long doctorId) {
        this.slots = slots != null ? slots : new ArrayList<>();
        this.doctorId = doctorId;
    }

    public List<CreateAvailabilityRequest> getSlots() {
        return slots;
    }

    public void setSlots(List<CreateAvailabilityRequest> slots) {
        this.slots = slots;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }
}
