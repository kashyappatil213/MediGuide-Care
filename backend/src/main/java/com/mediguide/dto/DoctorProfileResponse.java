package com.mediguide.dto;

import com.mediguide.model.DoctorProfile;

import java.time.LocalDateTime;

/**
 * Response DTO returned to an authenticated doctor viewing their own profile.
 */
public class DoctorProfileResponse {

    private Long id;
    private String publicId;
    private String fullName;
    private String email;
    private String specialty;
    private String clinicName;
    private String clinicAddress;
    private String city;
    private String area;
    private String pinCode;
    private String clinicPhone;
    private String personalPhone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DoctorProfileResponse() {
    }

    public DoctorProfileResponse(Long id, String publicId, String fullName, String email,
                                 String specialty, String clinicName, String clinicAddress,
                                 String city, String area, String pinCode,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, publicId, fullName, email, specialty, clinicName, clinicAddress, city, area, pinCode, null, null, createdAt, updatedAt);
    }

    public DoctorProfileResponse(Long id, String publicId, String fullName, String email,
                                 String specialty, String clinicName, String clinicAddress,
                                 String city, String area, String pinCode,
                                 String clinicPhone, String personalPhone,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.publicId = publicId;
        this.fullName = fullName;
        this.email = email;
        this.specialty = specialty;
        this.clinicName = clinicName;
        this.clinicAddress = clinicAddress;
        this.city = city;
        this.area = area;
        this.pinCode = pinCode;
        this.clinicPhone = clinicPhone;
        this.personalPhone = personalPhone;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DoctorProfileResponse fromEntity(DoctorProfile profile) {
        if (profile == null) {
            return null;
        }
        return new DoctorProfileResponse(
                profile.getId(),
                profile.getDoctor() != null ? profile.getDoctor().getPublicId() : null,
                profile.getDoctor() != null ? profile.getDoctor().getFullName() : null,
                profile.getDoctor() != null ? profile.getDoctor().getEmail() : null,
                profile.getSpecialty(),
                profile.getClinicName(),
                profile.getClinicAddress(),
                profile.getCity(),
                profile.getArea(),
                profile.getPinCode(),
                profile.getClinicPhone(),
                profile.getPersonalPhone(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getClinicName() {
        return clinicName;
    }

    public void setClinicName(String clinicName) {
        this.clinicName = clinicName;
    }

    public String getClinicAddress() {
        return clinicAddress;
    }

    public void setClinicAddress(String clinicAddress) {
        this.clinicAddress = clinicAddress;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getPinCode() {
        return pinCode;
    }

    public void setPinCode(String pinCode) {
        this.pinCode = pinCode;
    }

    public String getClinicPhone() {
        return clinicPhone;
    }

    public void setClinicPhone(String clinicPhone) {
        this.clinicPhone = clinicPhone;
    }

    public String getPersonalPhone() {
        return personalPhone;
    }

    public void setPersonalPhone(String personalPhone) {
        this.personalPhone = personalPhone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
