package com.mediguide.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating or updating a doctor's clinic and specialty profile.
 * Only authenticated doctors can submit this payload for their own profile.
 */
public class DoctorProfileRequest {

    @NotBlank(message = "Specialty is required")
    @Size(max = 100, message = "Specialty must not exceed 100 characters")
    private String specialty;

    @NotBlank(message = "Clinic name is required")
    @Size(max = 150, message = "Clinic name must not exceed 150 characters")
    private String clinicName;

    @Size(max = 255, message = "Clinic address must not exceed 255 characters")
    private String clinicAddress;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "Area must not exceed 100 characters")
    private String area;

    @Pattern(regexp = "^$|^[1-9][0-9]{5}$", message = "PIN code must be a valid 6-digit Indian postal code (e.g. 560001)")
    @Size(max = 10, message = "PIN code must not exceed 10 characters")
    private String pinCode;

    @Pattern(regexp = "^$|^[+]?[0-9\\s\\-()]{7,20}$", message = "Clinic contact phone number must be valid containing 7 to 20 digits")
    @Size(max = 30, message = "Clinic phone must not exceed 30 characters")
    private String clinicPhone;

    @Pattern(regexp = "^$|^[+]?[0-9\\s\\-()]{7,20}$", message = "Personal phone number must be valid containing 7 to 20 digits")
    @Size(max = 30, message = "Personal phone must not exceed 30 characters")
    private String personalPhone;

    public DoctorProfileRequest() {
    }

    public DoctorProfileRequest(String specialty, String clinicName, String clinicAddress,
                                String city, String area, String pinCode) {
        this(specialty, clinicName, clinicAddress, city, area, pinCode, null, null);
    }

    public DoctorProfileRequest(String specialty, String clinicName, String clinicAddress,
                                String city, String area, String pinCode,
                                String clinicPhone, String personalPhone) {
        this.specialty = specialty;
        this.clinicName = clinicName;
        this.clinicAddress = clinicAddress;
        this.city = city;
        this.area = area;
        this.pinCode = pinCode;
        this.clinicPhone = clinicPhone;
        this.personalPhone = personalPhone;
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
}
