package com.medibook.review.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/*
 * Review entity — maps to reviews table in MySQL.
 * Patient submits this after appointment is COMPLETED.
 * One review per appointment — enforced by unique constraint.
 * Rating updates doctor avgRating in providers table automatically.
 */
@Entity
@Table(name = "reviews",
       uniqueConstraints = @UniqueConstraint(columnNames = "appointment_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int reviewId;

    /*
     * Links to appointments table.
     * Unique constraint — one review per appointment only.
     */
    @Column(nullable = false, unique = true)
    private int appointmentId;

    /*
     * Who wrote this review?
     * Links to users table (UC1).
     */
    @Column(nullable = false)
    private int patientId;

    /*
     * Which doctor is being reviewed?
     * Links to providers table (UC2).
     * Used to recalculate avgRating.
     */
    @Column(nullable = false)
    private int providerId;

    /*
     * Star rating 1 to 5.
     * PDF defines exactly 1-5 range.
     * Used in avgRating calculation.
     */
    @Column(nullable = false)
    private int rating;

    /*
     * Written feedback from patient.
     * Optional — can be null.
     */
    private String comment;

    /*
     * Patient wants to stay anonymous?
     * true  = show as "Anonymous Patient"
     * false = show actual patient name
     * PDF privacy requirement.
     */
    @Column(nullable = false)
    private boolean isAnonymous = false;

    /*
     * Auto set when review first created.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /*
     * Auto updated every time review is edited.
     */
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}