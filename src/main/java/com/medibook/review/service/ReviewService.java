package com.medibook.review.service;

import com.medibook.review.dto.ReviewRequest;
import com.medibook.review.entity.Review;

import java.util.List;

/*
 * Service interface for Review.
 * Defines what operations are possible.
 * ReviewServiceImpl provides the actual logic.
 */
public interface ReviewService {

    /*
     * Patient submits a review after COMPLETED appointment.
     * Validates appointment is COMPLETED.
     * Checks no existing review for this appointment.
     * After saving → updates doctor avgRating in UC2.
     */
    Review submitReview(ReviewRequest request);

    /*
     * Get all reviews for a specific doctor.
     * Shown on doctor profile page.
     * Newest reviews shown first.
     */
    List<Review> getReviewsByProvider(int providerId);

    /*
     * Get all reviews written by a specific patient.
     * Shown on patient dashboard history.
     */
    List<Review> getReviewsByPatient(int patientId);

    /*
     * Get a single review by its ID.
     * Used for update and delete operations.
     */
    Review getReviewById(int reviewId);

    /*
     * Patient updates their own review.
     * Can change rating and comment.
     * After update → recalculates doctor avgRating.
     */
    Review updateReview(int reviewId, ReviewRequest request);

    /*
     * Admin deletes inappropriate review.
     * After delete → recalculates doctor avgRating.
     */
    void deleteReview(int reviewId);

    /*
     * Get average rating for a doctor.
     * Returns 0.0 if no reviews exist yet.
     * Used on doctor profile and search results.
     */
    double getAverageRating(int providerId);

    /*
     * Get total review count for a doctor.
     * Shown as "150 reviews" on doctor profile.
     */
    long getReviewCount(int providerId);
}