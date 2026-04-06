package com.medibook.review.repository;

import com.medibook.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/*
 * Repository for Review entity.
 * Handles all database queries for reviews table.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    /*
     * All reviews for a specific doctor.
     * Shown on doctor profile page.
     * Ordered newest first.
     */
    List<Review> findByProviderIdOrderByCreatedAtDesc(int providerId);

    /*
     * All reviews written by a specific patient.
     * Shown on patient dashboard.
     */
    List<Review> findByPatientId(int patientId);

    /*
     * Find review by appointmentId.
     * Checks if review already exists before allowing new one.
     * One appointment = one review only.
     */
    Optional<Review> findByAppointmentId(int appointmentId);

    /*
     * Find review by its own ID.
     * Used for update and delete operations.
     */
    Optional<Review> findByReviewId(int reviewId);

    /*
     * Calculate average rating for a doctor.
     * Called after every new review to update avgRating.
     * Returns null if no reviews exist yet.
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.providerId = :providerId")
    Double calculateAverageRatingByProviderId(
            @Param("providerId") int providerId
    );

    /*
     * Count total reviews for a doctor.
     * Shown on doctor profile: "150 reviews"
     */
    long countByProviderId(int providerId);

    /*
     * All reviews for a provider filtered by rating.
     * Example: show only 5 star reviews.
     * Used in admin moderation and patient filtering.
     */
    List<Review> findByProviderIdAndRating(int providerId, int rating);
}