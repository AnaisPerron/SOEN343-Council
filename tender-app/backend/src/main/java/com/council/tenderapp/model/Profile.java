package com.council.tenderapp.model;

import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stored lowercase to satisfy the chk_user_role constraint
    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, columnDefinition = "TEXT")
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String phone;

    protected Profile() {
    }

    public Profile(Role role, String name, String email, String passwordHash, String phone) {
        this.role = role.name().toLowerCase(Locale.ROOT);
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.phone = phone;
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return Role.valueOf(role.toUpperCase(Locale.ROOT));
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPhone() {
        return phone;
    }
}
