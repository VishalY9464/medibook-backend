package com.medibook.provider.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/*
 * This is the DTO (Data Transfer Object) for creating a provider profile.
 *
 * Think of it like a form a doctor fills when joining MediBook.
 * The doctor first registers as a normal User (UC1) and gets a userId.
 * Then they fill this form to create their professional doctor profile.
 *
 * Why a separate DTO and not directly use Provider entity?
 * Because we never expose our database entity directly to outside world.
 * DTO carries only what the client needs to send — nothing more nothing less.
 */

@Data
public class ProviderRequest {
	
	/*
     * This links the doctor profile to their user account.
     * Doctor registers first → gets userId → sends it here.
     * Without this we won't know which user is becoming a doctor.
     */
	
	@NotNull(message="User ID is required")
	private int userId;
	
	 /*
     * What type of doctor are they?
     * Example: Cardiologist, Dermatologist, Dentist, General Physician
     * Patients search by this field to find the right doctor.
     */
    @NotBlank(message = "Specialization is required")
    private String specialization;
    
    /*
     * Educational degree of the doctor.
     * Example: MBBS, MD, BDS, MS
     * Admin verifies this before approving the doctor on platform.
     */
    @NotBlank(message = "Qualification is required")
    private String qualification;
    
    /*
     * How many years of experience the doctor has.
     * Cannot be negative — that makes no sense.
     * Shown on doctor profile so patients can decide.
     */
    @Min(value = 0, message = "Experience years cannot be negative")
    private int experienceYears;
    
    /*
     * A short description about the doctor.
     * Example: "Specialist in heart diseases with 10 years experience..."
     * Optional field — doctor can add it later too.
     */
    private String bio;
    

    /*
     * Name of clinic or hospital where doctor works.
     * Example: Apollo Hospital, City Clinic
     * Shown on profile so patients know where to go.
     */
    @NotBlank(message = "Clinic name is required")
    private String clinicName;
    
    /*
     * Full address of the clinic.
     * Example: 123 MG Road, Bangalore - 560001
     * Patients need this to visit in person.
     */
    @NotBlank(message = "Clinic address is required")
    private String clinicAddress;
	
	

	
}
