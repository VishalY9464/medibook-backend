package com.medibook.review.resource;

import com.medibook.review.dto.ReviewRequest;
import com.medibook.review.entity.Review;
import com.medibook.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/*
 * REST Controller for Review.
 * Thin controller — all logic in ReviewServiceImpl.
 */
@RestController
@RequestMapping("/reviews")
public class ReviewResource {

    @Autowired
    private ReviewService reviewService;

    /*
     * Patient submits a review.
     * Who calls: Patient after appointment is COMPLETED
     * When: Patient clicks "Leave a Review" on their dashboard
     *
     * POST /reviews/submit
     */
    @PostMapping("/submit")
    public ResponseEntity<Review> submitReview(
            @Valid @RequestBody ReviewRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reviewService.submitReview(request));
    }

    /*
     * Get all reviews for a doctor.
     * Who calls: Patient viewing doctor profile
     * When: Patient clicks on a doctor to see reviews
     *
     * GET /reviews/provider/{providerId}
     */
    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<Review>> getByProvider(
            @PathVariable int providerId) {

        return ResponseEntity.ok(
                reviewService.getReviewsByProvider(providerId)
        );
    }

    /*
     * Get all reviews written by a patient.
     * Who calls: Patient viewing their own review history
     *
     * GET /reviews/patient/{patientId}
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Review>> getByPatient(
            @PathVariable int patientId) {

        return ResponseEntity.ok(
                reviewService.getReviewsByPatient(patientId)
        );
    }

    /*
     * Get a single review by ID.
     * Who calls: Admin or system
     *
     * GET /reviews/{reviewId}
     */
    @GetMapping("/{reviewId}")
    public ResponseEntity<Review> getById(
            @PathVariable int reviewId) {

        return ResponseEntity.ok(
                reviewService.getReviewById(reviewId)
        );
    }

    /*
     * Patient updates their review.
     * Who calls: Patient editing their review
     * When: Patient clicks "Edit Review" on dashboard
     *
     * PUT /reviews/{reviewId}
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<Review> updateReview(
            @PathVariable int reviewId,
            @Valid @RequestBody ReviewRequest request) {

        return ResponseEntity.ok(
                reviewService.updateReview(reviewId, request)
        );
    }

    /*
     * Admin deletes inappropriate review.
     * Who calls: Admin from moderation dashboard
     *
     * DELETE /reviews/{reviewId}
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @PathVariable int reviewId) {

        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(Map.of(
                "message", "Review deleted successfully."
        ));
    }

    /*
     * Get average rating for a doctor.
     * Who calls: Patient browsing doctor profiles
     * When: Doctor profile page loads
     *
     * GET /reviews/provider/{providerId}/average
     */
    @GetMapping("/provider/{providerId}/average")
    public ResponseEntity<?> getAverageRating(
            @PathVariable int providerId) {

        return ResponseEntity.ok(Map.of(
                "providerId", providerId,
                "averageRating", reviewService.getAverageRating(providerId)
        ));
    }

    /*
     * Get total review count for a doctor.
     * Who calls: Doctor profile page
     * When: Shows "150 reviews" on profile
     *
     * GET /reviews/provider/{providerId}/count
     */
    @GetMapping("/provider/{providerId}/count")
    public ResponseEntity<?> getReviewCount(
            @PathVariable int providerId) {

        return ResponseEntity.ok(Map.of(
                "providerId", providerId,
                "totalReviews", reviewService.getReviewCount(providerId)
        ));
    }
}