package com.medibook.review.service.impl;

import com.medibook.appointment.entity.Appointment;
import com.medibook.appointment.service.AppointmentService;
import com.medibook.exception.BadRequestException;
import com.medibook.exception.DuplicateResourceException;
import com.medibook.exception.ResourceNotFoundException;
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
 *
 * Exception handling:
 * → ResourceNotFoundException  → 404 when review not found
 * → DuplicateResourceException → 409 when review already exists
 * → BadRequestException        → 400 when business rule violated
 * All caught by GlobalExceptionHandler → clean JSON response always
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
     *    throws 404 if appointment not found
     *    throws 400 if appointment not COMPLETED
     * 2. Check no review already exists for this appointment
     *    throws 409 if review already exists
     * 3. Validate rating is between 1 and 5
     *    throws 400 if rating out of range
     * 4. Save review to database
     * 5. Recalculate and update doctor avgRating in UC2
     */
    @Override
    public Review submitReview(ReviewRequest request) {

        // step 1 — verify appointment exists and is COMPLETED
        // throws 404 Not Found if appointment not found
        Appointment appointment = appointmentService
                .getById(request.getAppointmentId());

        // appointment must be COMPLETED before reviewing
        // throws 400 Bad Request if not completed
        if (!appointment.getStatus().equals("COMPLETED")) {
            throw new BadRequestException(
                "You can only review after appointment is completed. " +
                "Current status: " + appointment.getStatus()
            );
        }

        // step 2 — check no review already exists
        // one appointment = one review only (PDF rule)
        // throws 409 Conflict if review already exists
        if (reviewRepository.findByAppointmentId(
                request.getAppointmentId()).isPresent()) {
            throw new DuplicateResourceException(
                "Review already exists for appointment: "
                + request.getAppointmentId()
            );
        }

        // step 3 — validate rating range
        // PDF defines exactly 1 to 5 stars only
        // throws 400 Bad Request if out of range
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException(
                "Rating must be between 1 and 5 stars."
            );
        }

        // step 4 — build and save review
        Review review = Review.builder()
                .appointmentId(request.getAppointmentId())
                .patientId(request.getPatientId())
                .providerId(request.getProviderId())
                .rating(request.getRating())
                .comment(request.getComment())
                .isAnonymous(request.isAnonymous())
                .build();

        Review saved = reviewRepository.save(review);

        // step 5 — recalculate and update doctor avgRating
        // this connects UC6 back to UC2 ProviderService
        updateDoctorRating(request.getProviderId());

        return saved;
    }

    /*
     * Get all reviews for a doctor.
     * Newest reviews shown first.
     * Shown on doctor profile page.
     */
    @Override
    public List<Review> getReviewsByProvider(int providerId) {
        return reviewRepository
                .findByProviderIdOrderByCreatedAtDesc(providerId);
    }

    /*
     * Get all reviews written by a patient.
     * Shown on patient dashboard history.
     */
    @Override
    public List<Review> getReviewsByPatient(int patientId) {
        return reviewRepository.findByPatientId(patientId);
    }

    /*
     * Get single review by ID.
     * Throws 404 Not Found if review does not exist.
     */
    @Override
    public Review getReviewById(int reviewId) {

        // throws 404 Not Found if review not found
        return reviewRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Review", "id", reviewId
                ));
    }

    /*
     * Patient updates their review.
     *
     * How it works:
     * 1. Find existing review — throws 404 if not found
     * 2. Validate new rating is between 1 and 5
     * 3. Update fields
     * 4. Save and recalculate doctor avgRating
     */
    @Override
    public Review updateReview(
            int reviewId, ReviewRequest request) {

        // find existing review — throws 404 if not found
        Review existing = getReviewById(reviewId);

        // validate new rating range
        // throws 400 Bad Request if out of range
        if (request.getRating() < 1
                || request.getRating() > 5) {
            throw new BadRequestException(
                "Rating must be between 1 and 5 stars."
            );
        }

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
     *
     * How it works:
     * 1. Find review — throws 404 if not found
     * 2. Get providerId before deleting
     * 3. Delete the review
     * 4. Recalculate doctor avgRating
     */
    @Override
    public void deleteReview(int reviewId) {

        // find review to get providerId before deleting
        // throws 404 Not Found if review not found
        Review review = getReviewById(reviewId);
        int providerId = review.getProviderId();

        // delete the review
        reviewRepository.deleteById(reviewId);

        // recalculate doctor rating after deletion
        // doctor rating goes up if bad review deleted
        updateDoctorRating(providerId);
    }

    /*
     * Get average rating for a doctor.
     * Returns 0.0 if doctor has no reviews yet.
     * Rounds to 1 decimal place.
     * Example: 4.333 → 4.3
     */
    @Override
    public double getAverageRating(int providerId) {

        Double avg = reviewRepository
                .calculateAverageRatingByProviderId(providerId);

        // return 0.0 if no reviews yet
        // round to 1 decimal place for clean display
        return avg != null
                ? Math.round(avg * 10.0) / 10.0
                : 0.0;
    }

    /*
     * Get total review count for a doctor.
     * Shown as "150 reviews" on doctor profile.
     */
    @Override
    public long getReviewCount(int providerId) {
        return reviewRepository.countByProviderId(providerId);
    }

    /*
     * Private helper — recalculates and updates doctor avgRating.
     * Called after every submit, update, and delete.
     * This connects UC6 back to UC2 ProviderService.
     *
     * Flow:
     * New review saved
     * → calculateAverageRating() from all reviews
     * → ProviderService.updateRating() updates provider table
     * → Doctor profile shows new avgRating instantly
     */
    private void updateDoctorRating(int providerId) {
        double newAvg = getAverageRating(providerId);
        providerService.updateRating(providerId, newAvg);
    }
}