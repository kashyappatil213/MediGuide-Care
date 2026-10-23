package com.mediguide.dto;

import com.mediguide.model.Role;
import com.mediguide.model.User;

/**
 * Safe public Data Transfer Object representing user details.
 * Strictly excludes passwords or password hashes.
 */
public class UserResponse {

    private Long id;
    private String publicId;
    private String fullName;
    private String email;
    private Role role;

    public UserResponse() {
    }

    public UserResponse(Long id, String fullName, String email, Role role) {
        this(id, null, fullName, email, role);
    }

    public UserResponse(Long id, String publicId, String fullName, String email, Role role) {
        this.id = id;
        this.publicId = publicId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    /**
     * Factory method creating a safe UserResponse from a User JPA entity.
     *
     * @param user user entity
     * @return safe UserResponse DTO
     */
    public static UserResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getPublicId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole()
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
