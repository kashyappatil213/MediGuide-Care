package com.mediguide.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * JPA entity representing a system user (Patient, Doctor, or Admin).
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 120, message = "Email must not exceed 120 characters")
    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @NotBlank(message = "Password hash is required")
    @Size(max = 255, message = "Password hash must not exceed 255 characters")
    @Column(nullable = false, length = 255)
    private String password;

    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "public_id", unique = true, updatable = false, length = 30)
    private String publicId;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    /**
     * Default no-argument constructor required by JPA.
     */
    public User() {
    }

    /**
     * Parameterized constructor for creating user instances.
     *
     * @param fullName full name of the user
     * @param email unique email address
     * @param password hashed password string
     * @param role assigned system role
     */
    public User(String fullName, String email, String password, Role role) {
        this(fullName, email, password, role, null);
    }

    /**
     * Parameterized constructor with publicId.
     *
     * @param fullName full name of the user
     * @param email unique email address
     * @param password hashed password string
     * @param role assigned system role
     * @param publicId unique public identifier
     */
    public User(String fullName, String email, String password, Role role, String publicId) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.role = role;
        this.publicId = publicId;
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        if (this.publicId == null && this.role != null) {
            this.publicId = com.mediguide.util.PublicIdGenerator.generateRandomPublicId(this.role);
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Omits password to prevent credential leakage in logs or debug traces.
     */
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", publicId='" + publicId + '\'' +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                '}';
    }
}
