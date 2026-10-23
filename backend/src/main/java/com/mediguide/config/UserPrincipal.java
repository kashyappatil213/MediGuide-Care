package com.mediguide.config;

import com.mediguide.model.Role;

/**
 * Lightweight principal object representing an authenticated user in Spring Security.
 */
public class UserPrincipal {

    private final Long id;
    private final String email;
    private final Role role;

    public UserPrincipal(Long id, String email, Role role) {
        this.id = id;
        this.email = email;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public String toString() {
        return "UserPrincipal{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", role=" + role +
                '}';
    }
}
