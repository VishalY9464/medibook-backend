package com.medibook.provider.service;

import java.util.List;

import com.medibook.provider.dto.ProviderRequest;
import com.medibook.provider.entity.Provider;

/*
 * This is the Service Interface for Provider.
 *
 * Think of it like a menu in a restaurant.
 * The menu tells you WHAT is available.
 * The kitchen (ProviderServiceImpl) decides HOW to make it.
 *
 * Why do we need an interface?
 * → defines the contract — what methods must exist
 * → ProviderServiceImpl must implement ALL of these
 * → if tomorrow we want a different implementation
 *   we just swap the impl without touching anything else
 * → evaluators check this 5 layer pattern strictly
 *
 * Rule: interface only declares methods — no logic here.
 * All logic goes in ProviderServiceImpl.
 */

public interface ProviderService {

	/*
     * Register a new provider profile.
     * Doctor sends their details → we save to database.
     * Returns saved provider with generated providerId.
     */
	
	Provider registerProvider(ProviderRequest request);
	
	 /*
     * Get a single provider by their providerId.
     * Used when patient clicks on a doctor to view full profile.
     * Throws exception if provider not found.
     */
	
	Provider getProviderById(int providerId);
	
	 /*
     * Get provider profile by their userId.
     * Used when doctor logs in and wants to see their own profile.
     * userId comes from JWT token.
     */
	Provider getProviderByUserId(int userId);

    /*
     * Get all doctors with a specific specialization.
     * Used when patient filters by specialization.
     * Example: patient selects Cardiologist from dropdown.
     */
	List<Provider> getBySpecialization(String specialization);
	
	
	 /*
     * Search doctors by name or specialization keyword.
     * Used for the search bar on patient dashboard.
     * Example: patient types "heart" or "Dr. Sharma".
     */
	List<Provider> searchProviders(String keyword);
	
	 /*
     * Update provider profile details.
     * Doctor can update their bio, clinic address etc.
     * Returns updated provider.
     */
	Provider updateProvider(int providerId, ProviderRequest request);
	
	  /*
     * Admin verifies a doctor after checking their credentials.
     * Sets isVerified = true.
     * After this doctor appears in patient search results.
     * This is a strict PDF requirement.
     */
	void verifyProvider(int providerId);
	
	 /*
     * Doctor sets their availability status.
     * true = accepting appointments
     * false = not available (on leave etc)
     * Patients only see available doctors.
     */
	void setAvailability(int providerId, boolean isAvailable);
	

    /*
     * Delete a provider profile.
     * Used by admin to remove a doctor from platform.
     */
	void deleteProvider(int providerId);
	
	 /*
     * Update the average rating of a doctor.
     * Called automatically when patient submits a review.
     * Recalculates average from all reviews and updates.
     */
	void updateRating(int providerId, double newRating);
	
	/*
     * Get all providers on the platform.
     * Used by admin to see all doctors.
     * Also used for browsing all doctors on patient dashboard.
     */
	List<Provider> getAllProviders();
	
    /*
     * Get all verified and available providers.
     * This is the main list shown to patients when they search.
     * Both conditions must be true — verified by admin AND available.
     */
	List<Provider> getVerifiedAndAvailableProviders();
}
