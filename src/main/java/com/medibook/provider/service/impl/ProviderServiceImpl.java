package com.medibook.provider.service.impl;

import com.medibook.exception.BadRequestException;
import com.medibook.exception.DuplicateResourceException;
import com.medibook.exception.ResourceNotFoundException;
import com.medibook.provider.dto.ProviderRequest;
import com.medibook.provider.entity.Provider;
import com.medibook.provider.repository.ProviderRepository;
import com.medibook.provider.service.ProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/*
 * This is the actual implementation of ProviderService.
 *
 * Think of it like the kitchen in a restaurant.
 * ProviderService (interface) is the menu — tells WHAT is available.
 * This class is the kitchen — decides HOW to prepare each item.
 *
 * All the real business logic lives here.
 * Every method from ProviderService interface must be implemented here.
 * If we miss any method — Java will give a compile error.
 *
 * @Service tells Spring to manage this class as a bean.
 * Spring will automatically inject it wherever ProviderService is needed.
 *
 * Exception handling:
 * → ResourceNotFoundException  → 404 when entity not found
 * → DuplicateResourceException → 409 when entity already exists
 * → BadRequestException        → 400 when business rule violated
 * All caught by GlobalExceptionHandler → clean JSON response always
 */
@Service
public class ProviderServiceImpl implements ProviderService {

    @Autowired
    private ProviderRepository providerRepository;

    /*
     * Register a new provider profile.
     *
     * How it works:
     * 1. Check if provider profile already exists for this userId
     *    one user cannot have two provider profiles
     * 2. Build Provider object from request data
     * 3. Set default values — isVerified=false, isAvailable=true
     * 4. Save to database and return
     *
     * Important: isVerified is false by default.
     * Doctor will NOT appear in search until admin verifies them.
     * This is a strict business rule from the PDF.
     */
    @Override
    public Provider registerProvider(ProviderRequest request) {

        // check if this user already has a provider profile
        // one user account can only have one doctor profile
        // throws 409 Conflict if profile already exists
        if (providerRepository.findByUserId(
                request.getUserId()).isPresent()) {
            throw new DuplicateResourceException(
                "Provider profile", "userId", request.getUserId()
            );
        }

        // build the provider object from the request data
        Provider provider = Provider.builder()
                .userId(request.getUserId())
                .specialization(request.getSpecialization())
                .qualification(request.getQualification())
                .experienceYears(request.getExperienceYears())
                .bio(request.getBio())
                .clinicName(request.getClinicName())
                .clinicAddress(request.getClinicAddress())
                // new doctor starts with zero rating
                // updates automatically when patients review
                .avgRating(0.0)
                // admin must verify before doctor appears in search
                // this is a PDF requirement
                .isVerified(false)
                // doctor is available by default when they join
                .isAvailable(true)
                .build();

        // save to database and return saved provider
        // saved provider will have auto generated providerId
        return providerRepository.save(provider);
    }

    /*
     * Get a single provider by their providerId.
     *
     * Used when patient clicks on a doctor to view full profile.
     * Throws 404 if provider not found.
     */
    @Override
    public Provider getProviderById(int providerId) {

        // find provider by ID
        // throws 404 Not Found if not found
        return providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Provider", "id", providerId
                ));
    }

    /*
     * Get provider profile by their userId.
     *
     * Doctor logs in → JWT gives us userId
     * We use that userId to find their provider profile.
     * Throws 404 if provider profile not found.
     */
    @Override
    public Provider getProviderByUserId(int userId) {

        // find provider by userId
        // throws 404 Not Found if not found
        return providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Provider", "userId", userId
                ));
    }

    /*
     * Get all doctors with a specific specialization.
     *
     * Patient selects Cardiologist from dropdown.
     * Returns only verified doctors with that specialization.
     * Unverified doctors never appear to patients — PDF rule.
     */
    @Override
    public List<Provider> getBySpecialization(String specialization) {

        // validate specialization is not empty
        if (specialization == null
                || specialization.trim().isEmpty()) {
            throw new BadRequestException(
                "Specialization cannot be empty."
            );
        }

        // get all doctors with this specialization
        // filter to show only verified ones to patients
        return providerRepository
                .findBySpecialization(specialization)
                .stream()
                .filter(Provider::isVerified)
                .toList();
    }

    /*
     * Search doctors by name or specialization keyword.
     *
     * Patient types anything in search bar.
     * We search both doctor name and specialization.
     * Returns matching results.
     *
     * Example:
     * "heart" → finds all Cardiologists
     * "Sharma" → finds Dr. Sharma
     * "skin" → finds Dermatologists
     */
    @Override
    public List<Provider> searchProviders(String keyword) {

        // validate keyword is not empty
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BadRequestException(
                "Search keyword cannot be empty."
            );
        }

        // use custom @Query in repository
        // searches both name and specialization in one query
        return providerRepository
                .searchByNameOrSpecialization(keyword);
    }

    /*
     * Update provider profile details.
     *
     * How it works:
     * 1. Find existing provider by ID — throws 404 if not found
     * 2. Update only the fields that came in request
     * 3. Save and return updated provider
     *
     * Doctor cannot update isVerified or avgRating directly.
     * Those are updated by separate methods.
     */
    @Override
    public Provider updateProvider(
            int providerId, ProviderRequest request) {

        // find existing provider — throws 404 if not found
        Provider existing = getProviderById(providerId);

        // validate experience years is not negative
        if (request.getExperienceYears() < 0) {
            throw new BadRequestException(
                "Experience years cannot be negative."
            );
        }

        // update only the fields doctor is allowed to change
        // doctor cannot change their own verification status
        existing.setSpecialization(request.getSpecialization());
        existing.setQualification(request.getQualification());
        existing.setExperienceYears(request.getExperienceYears());
        existing.setBio(request.getBio());
        existing.setClinicName(request.getClinicName());
        existing.setClinicAddress(request.getClinicAddress());

        // save updated provider to database and return
        return providerRepository.save(existing);
    }

    /*
     * Admin verifies a doctor after checking their credentials.
     *
     * How it works:
     * 1. Admin reviews doctor qualifications
     * 2. Admin calls this method to approve
     * 3. isVerified becomes true
     * 4. Doctor now appears in patient search results
     *
     * This is a strict PDF requirement.
     * Unverified doctor = invisible to patients.
     */
    @Override
    public void verifyProvider(int providerId) {

        // find the provider — throws 404 if not found
        Provider provider = getProviderById(providerId);

        // check if already verified — no need to verify twice
        if (provider.isVerified()) {
            throw new BadRequestException(
                "Provider is already verified."
            );
        }

        // set verified to true — doctor is now approved
        provider.setVerified(true);

        // save the updated status to database
        providerRepository.save(provider);
    }

    /*
     * Doctor sets their own availability status.
     *
     * Doctor going on leave → sets isAvailable = false
     * Doctor comes back → sets isAvailable = true
     * Patients only see available doctors in search.
     */
    @Override
    public void setAvailability(int providerId, boolean isAvailable) {

        // find the provider — throws 404 if not found
        Provider provider = getProviderById(providerId);

        // update availability status
        provider.setAvailable(isAvailable);

        // save to database
        providerRepository.save(provider);
    }

    /*
     * Delete a provider profile from the platform.
     *
     * Admin decides to remove a doctor.
     * Deletes only provider profile.
     * User account in users table is separate.
     */
    @Override
    public void deleteProvider(int providerId) {

        // check provider exists before deleting
        // throws 404 if not found
        getProviderById(providerId);

        // delete from database
        providerRepository.deleteById(providerId);
    }

    /*
     * Update the average rating of a doctor.
     *
     * Called from ReviewServiceImpl (UC6)
     * every time a new review is submitted or deleted.
     * Validates rating is within 0 to 5 range.
     */
    @Override
    public void updateRating(int providerId, double newRating) {

        // validate rating range
        if (newRating < 0.0 || newRating > 5.0) {
            throw new BadRequestException(
                "Rating must be between 0.0 and 5.0."
            );
        }

        // find the provider — throws 404 if not found
        Provider provider = getProviderById(providerId);

        // update the average rating
        provider.setAvgRating(newRating);

        // save to database
        providerRepository.save(provider);
    }

    /*
     * Get all providers on the platform.
     *
     * Used by admin to see complete list of all doctors.
     * Admin can see both verified and unverified doctors.
     * Patients only see verified doctors via different method.
     */
    @Override
    public List<Provider> getAllProviders() {

        // return all providers — no filter
        // admin sees everything
        return providerRepository.findAll();
    }

    /*
     * Get all verified and available providers.
     *
     * Main method used for patient search.
     * Both conditions must be true:
     * → isVerified = true (admin approved)
     * → isAvailable = true (doctor accepting patients)
     *
     * This is what patients see when they browse doctors.
     */
    @Override
    public List<Provider> getVerifiedAndAvailableProviders() {

        // only return doctors who are both verified AND available
        // this is the patient facing search result
        return providerRepository
                .findByIsVerifiedAndIsAvailable(true, true);
    }
}