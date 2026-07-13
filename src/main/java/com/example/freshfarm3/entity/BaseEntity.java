package com.example.freshfarm3.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;
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
 *
 * equals()/hashCode() are implemented here (identity-based, on id) so that
 * ALL entities extending BaseEntity behave correctly as Map/Set keys and
 * in stream grouping (Collectors.groupingBy, HashMap.merge, etc). Without
 * this, two managed instances representing the same DB row compare as
 * unequal, silently corrupting any code that groups/dedupes entities.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        Class<?> thisEffectiveClass = (this instanceof HibernateProxy)
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        Class<?> otherEffectiveClass = (o instanceof HibernateProxy)
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();

        if (thisEffectiveClass != otherEffectiveClass) return false;

        BaseEntity other = (BaseEntity) o;
        // Entities with a null id (not yet persisted) are only equal by reference,
        // which the `this == o` check above already covers.
        return id != null && id.equals(other.getId());
    }

    @Override
    public final int hashCode() {
        return (this instanceof HibernateProxy)
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }

    public void setAverageRating(double v) {
    }

    public void setReviewCount(int count) {
    }
}