package com.mediguide.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * JPA entity representing professional, clinic, and location details for a doctor.
 * Maintained as an independent entity with a 1-to-1 relationship to the User entity
 * to keep core authentication clean, avoid sparse columns on users, and safeguard
 * against accidental credential leakage during public doctor searches.
 */
@Entity
@Table(name = "doctor_profiles")
public class DoctorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Doctor user reference is required")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", unique = true, nullable = false)
    private User doctor;

    @NotBlank(message = "Medical specialty is required")
    @Size(max = 100, message = "Specialty must not exceed 100 characters")
    @Column(name = "specialty", nullable = false, length = 100)
    private String specialty;

    @NotBlank(message = "Clinic name is required")
    @Size(max = 150, message = "Clinic name must not exceed 150 characters")
    @Column(name = "clinic_name", nullable = false, length = 150)
    private String clinicName;

    @Size(max = 255, message = "Clinic address must not exceed 255 characters")
    @Column(name = "clinic_address", length = 255)
    private String clinicAddress;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Size(max = 100, message = "Area or locality must not exceed 100 characters")
    @Column(name = "area", length = 100)
    private String area;

    @Pattern(regexp = "^$|^[1-9][0-9]{5}$", message = "PIN code must be a valid 6-digit Indian postal code (e.g. 560001)")
    @Size(max = 10, message = "PIN code must not exceed 10 characters")
    @Column(name = "pin_code", length = 10)
    private String pinCode;

    @Column(name = "clinic_phone", length = 30)
    private String clinicPhone;

    @Column(name = "personal_phone", length = 30)
    private String personalPhone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public DoctorProfile() {
    }

    public DoctorProfile(User doctor, String specialty, String clinicName, String clinicAddress,
                         String city, String area, String pinCode) {
        this(doctor, specialty, clinicName, clinicAddress, city, area, pinCode, null, null);
    }

    public DoctorProfile(User doctor, String specialty, String clinicName, String clinicAddress,
                         String city, String area, String pinCode, String clinicPhone, String personalPhone) {
        this.doctor = doctor;
        this.specialty = specialty;
        this.clinicName = clinicName;
        this.clinicAddress = clinicAddress;
        this.city = city;
        this.area = area;
        this.pinCode = pinCode;
        this.clinicPhone = clinicPhone;
        this.personalPhone = personalPhone;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getDoctor() {
        return doctor;
    }

    public void setDoctor(User doctor) {
        this.doctor = doctor;
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
