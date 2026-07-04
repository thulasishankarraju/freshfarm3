package com.example.freshfarm3.entity;

import com.example.freshfarm3.enums.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "phone", nullable = false, unique = true, length = 15)
    private String phone;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    /**
     * Farmers start inactive until Admin approves.
     * Buyers are active immediately after registration.
     */
    @Builder.Default  // fix: @Builder ignores field defaults without this
    @Column(name = "active", nullable = false)
    private boolean active = true;
}