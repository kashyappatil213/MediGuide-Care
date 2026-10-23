package com.mediguide.dto;

import com.mediguide.model.DoctorProfile;

/**
 * Public response DTO returned to patients and search queries.
 * Exposes only safe, public clinical information:
 * - Public MediGuide ID
 * - Doctor Name
 * - Specialty
 * - Clinic Name
 * - Clinic Address
 * - City
 * - Area
 * - PIN Code
 *
 * Absolutely never exposes email, password hashes, or internal database primary keys.
 */
public class PublicDoctorProfileResponse {

    private String publicId;
    private String doctorName;
    private String specialty;
    private String clinicName;
    private String clinicAddress;
    private String city;
    private String area;
    private String pinCode;
    private String clinicPhone;

    public PublicDoctorProfileResponse() {
    }

    public PublicDoctorProfileResponse(String publicId, String doctorName, String specialty,
                                       String clinicName, String clinicAddress,
                                       String city, String area, String pinCode) {
        this(publicId, doctorName, specialty, clinicName, clinicAddress, city, area, pinCode, null);
    }

    public PublicDoctorProfileResponse(String publicId, String doctorName, String specialty,
                                       String clinicName, String clinicAddress,
                                       String city, String area, String pinCode,
                                       String clinicPhone) {
        this.publicId = publicId;
        this.doctorName = doctorName;
        this.specialty = specialty;
        this.clinicName = clinicName;
        this.clinicAddress = clinicAddress;
        this.city = city;
        this.area = area;
        this.pinCode = pinCode;
        this.clinicPhone = clinicPhone;
    }

    public static PublicDoctorProfileResponse fromEntity(DoctorProfile profile) {
        if (profile == null) {
            return null;
        }
        return new PublicDoctorProfileResponse(
                profile.getDoctor() != null ? profile.getDoctor().getPublicId() : null,
                profile.getDoctor() != null ? profile.getDoctor().getFullName() : null,
                profile.getSpecialty(),
                profile.getClinicName(),
                profile.getClinicAddress(),
                profile.getCity(),
                profile.getArea(),
                profile.getPinCode(),
                profile.getClinicPhone()
        );
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
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
}
