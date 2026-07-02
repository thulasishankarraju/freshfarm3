package com.example.freshfarm3.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * BaseEntity — Shared audit fields for all entities.
 *
 * Every table that extends this will automatically get:
 *   - id (auto-generated primary key)
 *   - createdAt (when record was created)
 *   - updatedAt (when record was last modified)
 *
 * @MappedSuperclass — JPA does NOT create a table for this class.
 * It merges its fields into every child entity's table.
 *
 * NOTE: @EnableJpaAuditing must be present on your main application
 * class for @CreatedDate / @LastModifiedDate to populate automatically.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)  // fix 1: explicit @Column
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();  // fix 2: Java-level fallback default

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}