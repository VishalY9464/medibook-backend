package com.medibook.provider.service.impl;

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
 */
@Service
public class ProviderServiceImpl implements ProviderService {

    /*
     * We inject ProviderRepository here.
     * This gives us all the database query methods we defined.
     * @Autowired tells Spring to automatically provide this dependency.
     * We never create ProviderRepository manually — Spring handles it.
     */
    @Autowired
    private ProviderRepository providerRepository;

    /*
     * Register a new provider profile.
     *
     * How it works:
     * 1. Check if provider profile already exists for this userId
     *    (one user cannot have two provider profiles)
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
        if (providerRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new RuntimeException(
                "Provider profile already exists for this user"
            );
        }

        // build the provider object from the request data
        // we use builder pattern because Provider has @Builder annotation
        Provider provider = Provider.builder()
                .userId(request.getUserId())
                .specialization(request.getSpecialization())
                .qualification(request.getQualification())
                .experienceYears(request.getExperienceYears())
                .bio(request.getBio())
                .clinicName(request.getClinicName())
                .clinicAddress(request.getClinicAddress())
                // new doctor starts with zero rating
                // this updates automatically when patients review
                .avgRating(0.0)
                // admin must verify before doctor appears in search
                // this is a PDF requirement
                .isVerified(false)
                // doctor is available by default when they join
                .isAvailable(true)
                .build();

        // save to database and return the saved provider
        // saved provider will have the auto generated providerId
        return providerRepository.save(provider);
    }

    /*
     * Get a single provider by their providerId.
     *
     * How it works:
     * 1. Call repository to find provider by ID
     * 2. If not found — throw exception with clear message
     * 3. If found — return the provider object
     *
     * Used when patient clicks on a doctor to view full profile.
     */
    @Override
    public Provider getProviderById(int providerId) {

        // find provider by ID or throw exception if not found
        // orElseThrow gives a clean error instead of returning null
        return providerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException(
                    "Provider not found with ID: " + providerId
                ));
    }

    /*
     * Get provider profile by their userId.
     *
     * How it works:
     * Doctor logs in → JWT gives us userId
     * We use that userId to find their provider profile
     *
     * Different from getProviderById because:
     * providerId = provider table ID
     * userId = user table ID (from login)
     */
    @Override
    public Provider getProviderByUserId(int userId) {

        // find provider by userId or throw exception
        return providerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException(
                    "Provider profile not found for user ID: " + userId
                ));
    }

    /*
     * Get all doctors with a specific specialization.
     *
     * How it works:
     * Patient selects Cardiologist from dropdown
     * We return all verified doctors with that specialization
     *
     * We filter by isVerified=true because
     * unverified doctors should never appear to patients.
     */
    @Override
    public List<Provider> getBySpecialization(String specialization) {

        // get all doctors with this specialization
        // then filter to show only verified ones to patients
        return providerRepository.findBySpecialization(specialization)
                .stream()
                .filter(Provider::isVerified)
                .toList();
    }

    /*
     * Search doctors by name or specialization keyword.
     *
     * How it works:
     * Patient types anything in search bar
     * We search both doctor name and specialization
     * Returns matching results
     *
     * Example:
     * "heart" → finds all Cardiologists
     * "Sharma" → finds Dr. Sharma
     * "skin" → finds Dermatologists
     */
    @Override
    public List<Provider> searchProviders(String keyword) {

        // use the custom @Query we wrote in repository
        // searches both name and specialization in one query
        return providerRepository.searchByNameOrSpecialization(keyword);
    }

    /*
     * Update provider profile details.
     *
     * How it works:
     * 1. Find existing provider by ID
     * 2. Update only the fields that came in request
     * 3. Save and return updated provider
     *
     * Note: we do not update isVerified or avgRating here
     * those are updated by separate methods
     */
    @Override
    public Provider updateProvider(int providerId, ProviderRequest request) {

        // first find the existing provider
        // throws exception if not found
        Provider existing = getProviderById(providerId);

        // update only the fields that doctor is allowed to change
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

        // find the provider first
        Provider provider = getProviderById(providerId);

        // set verified to true — doctor is now approved
        provider.setVerified(true);

        // save the updated status to database
        providerRepository.save(provider);
    }

    /*
     * Doctor sets their own availability status.
     *
     * How it works:
     * Doctor going on leave → sets isAvailable = false
     * Doctor comes back → sets isAvailable = true
     * Patients only see available doctors in search
     */
    @Override
    public void setAvailability(int providerId, boolean isAvailable) {

        // find the provider first
        Provider provider = getProviderById(providerId);

        // update availability status
        provider.setAvailable(isAvailable);

        // save to database
        providerRepository.save(provider);
    }

    /*
     * Delete a provider profile from the platform.
     *
     * How it works:
     * Admin decides to remove a doctor
     * We delete their provider profile from database
     *
     * Note: this only deletes provider profile
     * their user account in users table is separate
     */
    @Override
    public void deleteProvider(int providerId) {

        // check provider exists before deleting
        // throws exception if not found
        getProviderById(providerId);

        // delete from database
        providerRepository.deleteById(providerId);
    }

    /*
     * Update the average rating of a doctor.
     *
     * How it works:
     * Patient submits a review → ReviewService calls this
     * We update the avgRating field on provider
     *
     * This is called from ReviewServiceImpl (UC6)
     * every time a new review is submitted.
     */
    @Override
    public void updateRating(int providerId, double newRating) {

        // find the provider first
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
     * Patients only see verified doctors (different method).
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
     * This is the main method used for patient search.
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
        return providerRepository.findByIsVerifiedAndIsAvailable(true, true);
    }
}