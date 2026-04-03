package com.medibook.provider.resource;

import com.medibook.provider.dto.ProviderRequest;
import com.medibook.provider.entity.Provider;
import com.medibook.provider.service.ProviderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/*
 * This is the REST Controller for Provider.
 *
 * Think of it like the front desk of a hotel.
 * Guest (client) comes to front desk (this controller)
 * Front desk calls the relevant department (service)
 * Department does the work and sends response back.
 *
 * This class only does two things:
 * 1. Receive the HTTP request
 * 2. Call the right service method and return response
 *
 * No business logic here — that lives in ProviderServiceImpl.
 * Keeping controller thin and service thick is good practice.
 *
 * @RestController → this class handles REST API requests
 * @RequestMapping → all URLs in this class start with /providers
 */
@RestController
@RequestMapping("/providers")
public class ProviderResource {

    /*
     * Inject ProviderService — Spring does this automatically.
     * We depend on the interface not the implementation.
     * This is loose coupling — a good design principle.
     */
    @Autowired
    private ProviderService providerService;

    /*
     * Register a new provider profile.
     *
     * Who calls this: Doctor after registering as user (UC1)
     * What happens: Creates doctor profile in providers table
     * Why POST: We are creating a new resource
     *
     * URL: POST /providers/register
     * Body: ProviderRequest (userId, specialization, qualification etc)
     * Returns: 201 Created with saved provider details
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerProvider(
            @Valid @RequestBody ProviderRequest request) {

        // call service to register provider
        Provider provider = providerService.registerProvider(request);

        // return 201 Created with provider details
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Provider profile created successfully." +
                                   " Waiting for admin verification.",
                        "providerId", provider.getProviderId(),
                        "isVerified", provider.isVerified()
                ));
    }

    /*
     * Get a single provider by their providerId.
     *
     * Who calls this: Patient clicking on a doctor profile
     * What happens: Returns full doctor profile
     * Why GET: We are reading a resource
     *
     * URL: GET /providers/{providerId}
     * Returns: 200 OK with provider details
     */
    @GetMapping("/{providerId}")
    public ResponseEntity<Provider> getById(
            @PathVariable int providerId) {

        // call service to get provider by id
        return ResponseEntity.ok(
                providerService.getProviderById(providerId)
        );
    }

    /*
     * Get provider profile by userId.
     *
     * Who calls this: Doctor viewing their own profile
     * What happens: Returns doctor profile using userId from JWT
     * Why GET: We are reading a resource
     *
     * URL: GET /providers/user/{userId}
     * Returns: 200 OK with provider details
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Provider> getByUserId(
            @PathVariable int userId) {

        // call service to get provider by userId
        return ResponseEntity.ok(
                providerService.getProviderByUserId(userId)
        );
    }

    /*
     * Get all doctors with a specific specialization.
     *
     * Who calls this: Patient filtering by specialization
     * What happens: Returns all verified doctors of that type
     * Why GET: We are reading resources
     *
     * URL: GET /providers/specialization/{specialization}
     * Example: GET /providers/specialization/Cardiologist
     * Returns: 200 OK with list of providers
     */
    @GetMapping("/specialization/{specialization}")
    public ResponseEntity<List<Provider>> getBySpecialization(
            @PathVariable String specialization) {

        // call service to get providers by specialization
        return ResponseEntity.ok(
                providerService.getBySpecialization(specialization)
        );
    }

    /*
     * Search doctors by name or specialization keyword.
     *
     * Who calls this: Patient using search bar
     * What happens: Returns matching verified doctors
     * Why GET: We are reading resources
     *
     * URL: GET /providers/search?keyword=heart
     * Example: GET /providers/search?keyword=Sharma
     * Returns: 200 OK with list of matching providers
     */
    @GetMapping("/search")
    public ResponseEntity<List<Provider>> search(
            @RequestParam String keyword) {

        // call service to search providers
        return ResponseEntity.ok(
                providerService.searchProviders(keyword)
        );
    }

    /*
     * Get all verified and available providers.
     *
     * Who calls this: Patient opening find a doctor page
     * What happens: Returns all doctors patient can book
     * Why GET: We are reading resources
     *
     * URL: GET /providers/available
     * Returns: 200 OK with list of available providers
     */
    @GetMapping("/available")
    public ResponseEntity<List<Provider>> getAvailable() {

        // call service to get verified and available providers
        return ResponseEntity.ok(
                providerService.getVerifiedAndAvailableProviders()
        );
    }

    /*
     * Get all providers on the platform.
     *
     * Who calls this: Admin viewing all doctors
     * What happens: Returns everyone including unverified
     * Why GET: We are reading resources
     *
     * URL: GET /providers/all
     * Returns: 200 OK with all providers
     */
    @GetMapping("/all")
    public ResponseEntity<List<Provider>> getAll() {

        // call service to get all providers
        return ResponseEntity.ok(
                providerService.getAllProviders()
        );
    }

    /*
     * Update provider profile.
     *
     * Who calls this: Doctor updating their own profile
     * What happens: Updates bio, clinic, specialization etc
     * Why PUT: We are updating an existing resource
     *
     * URL: PUT /providers/{providerId}
     * Body: ProviderRequest with updated details
     * Returns: 200 OK with updated provider
     */
    @PutMapping("/{providerId}")
    public ResponseEntity<Provider> updateProvider(
            @PathVariable int providerId,
            @Valid @RequestBody ProviderRequest request) {

        // call service to update provider
        return ResponseEntity.ok(
                providerService.updateProvider(providerId, request)
        );
    }

    /*
     * Admin verifies a doctor.
     *
     * Who calls this: Admin after reviewing doctor credentials
     * What happens: Sets isVerified=true
     *               Doctor now appears in patient search
     * Why PUT: We are updating an existing resource
     *
     * URL: PUT /providers/{providerId}/verify
     * Returns: 200 OK with success message
     */
    @PutMapping("/{providerId}/verify")
    public ResponseEntity<?> verifyProvider(
            @PathVariable int providerId) {

        // call service to verify provider
        providerService.verifyProvider(providerId);

        // return success message
        return ResponseEntity.ok(Map.of(
                "message", "Provider verified successfully." +
                           " Doctor is now visible to patients."
        ));
    }

    /*
     * Doctor sets their availability status.
     *
     * Who calls this: Doctor going on leave or coming back
     * What happens: Updates isAvailable flag
     * Why PUT: We are updating an existing resource
     *
     * URL: PUT /providers/{providerId}/availability?isAvailable=false
     * Returns: 200 OK with success message
     */
    @PutMapping("/{providerId}/availability")
    public ResponseEntity<?> setAvailability(
            @PathVariable int providerId,
            @RequestParam boolean isAvailable) {

        // call service to update availability
        providerService.setAvailability(providerId, isAvailable);

        // return appropriate message based on new status
        String message = isAvailable
                ? "Doctor is now available for appointments."
                : "Doctor is now unavailable for appointments.";

        return ResponseEntity.ok(Map.of("message", message));
    }

    /*
     * Delete a provider profile.
     *
     * Who calls this: Admin removing a doctor from platform
     * What happens: Deletes provider profile from database
     * Why DELETE: We are deleting a resource
     *
     * URL: DELETE /providers/{providerId}
     * Returns: 200 OK with success message
     */
    @DeleteMapping("/{providerId}")
    public ResponseEntity<?> deleteProvider(
            @PathVariable int providerId) {

        // call service to delete provider
        providerService.deleteProvider(providerId);

        // return success message
        return ResponseEntity.ok(Map.of(
                "message", "Provider deleted successfully."
        ));
    }
}