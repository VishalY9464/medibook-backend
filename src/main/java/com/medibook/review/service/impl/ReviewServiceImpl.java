package com.medibook.review.service.impl;

import com.medibook.appointment.entity.Appointment;
import com.medibook.appointment.service.AppointmentService;
import com.medibook.provider.service.ProviderService;
import com.medibook.review.dto.ReviewRequest;
import com.medibook.review.entity.Review;
import com.medibook.review.repository.ReviewRepository;
import com.medibook.review.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/*
 * Business logic for Review Service.
 *
 * Key connections to other services:
 * → AppointmentService (UC4) → verify appointment is COMPLETED
 * → ProviderService (UC2) → update doctor avgRating after review
 *
 * PDF rules strictly followed:
 * → Only COMPLETED appointments can be reviewed
 * → One review per appointment
 * → Rating updates doctor avgRating automatically
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    /*
     * UC4 — verify appointment exists and is COMPLETED
     */
    @Autowired
    private AppointmentService appointmentService;

    /*
     * UC2 — update doctor avgRating after every review change
     */
    @Autowired
    private ProviderService providerService;

    /*
     * Submit a new review.
     *
     * How it works:
     * 1. Verify appointment exists and is COMPLETED
     * 2. Check no review already exists for this appointment
     * 3. Save review to database
     * 4. Recalculate and update doctor avgRating in UC2
     */
    @Override
    public Review submitReview(ReviewRequest request) {

        // step 1 — verify appointment exists and is COMPLETED
        // only completed appointments can be reviewed
        Appointment appointment = appointmentService
                .getById(request.getAppointmentId());

        if (!appointment.getStatus().equals("COMPLETED")) {
            throw new RuntimeException(
                "You can only review after appointment is completed. " +
                "Current status: " + appointment.getStatus()
            );
        }

        // step 2 — check no review already exists
        // one appointment = one review only (PDF rule)
        if (reviewRepository.findByAppointmentId(
                request.getAppointmentId()).isPresent()) {
            throw new RuntimeException(
                "You have already submitted a review for this appointment."
            );
        }

        // step 3 — build and save review
        Review review = Review.builder()
                .appointmentId(request.getAppointmentId())
                .patientId(request.getPatientId())
                .providerId(request.getProviderId())
                .rating(request.getRating())
                .comment(request.getComment())
                .isAnonymous(request.isAnonymous())
                .build();

        Review saved = reviewRepository.save(review);

        // step 4 — recalculate and update doctor avgRating
        // this is why ProviderService.updateRating() was built in UC2
        updateDoctorRating(request.getProviderId());

        return saved;
    }

    /*
     * Get all reviews for a doctor.
     * Newest reviews shown first.
     */
    @Override
    public List<Review> getReviewsByProvider(int providerId) {
        return reviewRepository
                .findByProviderIdOrderByCreatedAtDesc(providerId);
    }

    /*
     * Get all reviews by a patient.
     */
    @Override
    public List<Review> getReviewsByPatient(int patientId) {
        return reviewRepository.findByPatientId(patientId);
    }

    /*
     * Get single review by ID.
     */
    @Override
    public Review getReviewById(int reviewId) {
        return reviewRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new RuntimeException(
                    "Review not found with id: " + reviewId
                ));
    }

    /*
     * Patient updates their review.
     * After update → recalculates doctor avgRating.
     */
    @Override
    public Review updateReview(int reviewId, ReviewRequest request) {

        // find existing review
        Review existing = getReviewById(reviewId);

        // update fields
        existing.setRating(request.getRating());
        existing.setComment(request.getComment());
        existing.setAnonymous(request.isAnonymous());

        // save updated review
        Review saved = reviewRepository.save(existing);

        // recalculate doctor rating after update
        updateDoctorRating(existing.getProviderId());

        return saved;
    }

    /*
     * Admin deletes inappropriate review.
     * After delete → recalculates doctor avgRating.
     */
    @Override
    public void deleteReview(int reviewId) {

        // find review to get providerId before deleting
        Review review = getReviewById(reviewId);
        int providerId = review.getProviderId();

        // delete the review
        reviewRepository.deleteById(reviewId);

        // recalculate doctor rating after deletion
        updateDoctorRating(providerId);
    }

    /*
     * Get average rating for a doctor.
     * Returns 0.0 if doctor has no reviews yet.
     */
    @Override
    public double getAverageRating(int providerId) {
        Double avg = reviewRepository
                .calculateAverageRatingByProviderId(providerId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    /*
     * Get total review count for a doctor.
     */
    @Override
    public long getReviewCount(int providerId) {
        return reviewRepository.countByProviderId(providerId);
    }

    /*
     * Private helper — recalculates and updates doctor avgRating.
     * Called after every submit, update, and delete.
     * This connects UC6 back to UC2 ProviderService.
     */
    private void updateDoctorRating(int providerId) {
        double newAvg = getAverageRating(providerId);
        providerService.updateRating(providerId, newAvg);
    }
}