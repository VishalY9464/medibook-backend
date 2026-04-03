package com.medibook.provider.repository;

import com.medibook.provider.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/*
 * This is the Repository for Provider entity.
 *
 * Think of it like a helper that knows how to talk to the database.
 * We define what queries we need here and Spring Data JPA
 * automatically writes the SQL for us behind the scenes.
 *
 * We never write SQL manually — Spring figures it out
 * from the method names we write here.
 *
 * Example:
 * findBySpecialization("Cardiologist")
 * Spring converts this to:
 * SELECT * FROM providers WHERE specialization = 'Cardiologist'
 */
@Repository
public interface ProviderRepository extends JpaRepository<Provider, Integer> {

    /*
     * Find provider profile by their userId.
     * Used when doctor logs in and wants to see their profile.
     * Also used to check if provider profile already exists.
     * Returns Optional because provider may not exist yet.
     */
    Optional<Provider> findByUserId(int userId);

    /*
     * Find all doctors with a specific specialization.
     * Used when patient searches "show me all Cardiologists".
     * Returns a list because multiple doctors can have same specialization.
     * Only verified doctors should be shown — filtered in service layer.
     */
    List<Provider> findBySpecialization(String specialization);

    /*
     * Find all verified doctors on the platform.
     * isVerified = true means admin has approved them.
     * Used when patient browses all available doctors.
     * Unverified doctors never appear in patient search.
     */
    List<Provider> findByIsVerified(boolean isVerified);

    /*
     * Find all doctors who are currently available.
     * isAvailable = true means doctor is accepting appointments.
     * Doctor can turn this off when on leave.
     */
    List<Provider> findByIsAvailable(boolean isAvailable);

    /*
     * Search doctors by name OR specialization.
     * Used for the search bar on patient dashboard.
     * Example: patient types "heart" → finds cardiologists
     * Example: patient types "Dr. Sharma" → finds that doctor
     *
     * We use @Query here because this needs a JOIN with users table
     * to search by doctor name (name is in users table not providers).
     * LOWER() makes search case insensitive.
     */
    @Query("SELECT p FROM Provider p JOIN User u ON p.userId = u.userId " +
           "WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.specialization) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Provider> searchByNameOrSpecialization(@Param("keyword") String keyword);

    /*
     * Find doctors by clinic address or location.
     * Used when patient searches "doctors near Bangalore".
     * Searches if the address contains the location keyword.
     */
    List<Provider> findByClinicAddressContaining(String location);

    /*
     * Count how many doctors exist for each specialization.
     * Used in admin analytics dashboard.
     * Example: "We have 15 Cardiologists on platform"
     */
    long countBySpecialization(String specialization);

    /*
     * Find all verified AND available doctors.
     * This is the main query for patient search results.
     * Only doctors who are verified by admin AND
     * currently available appear in search.
     */
    List<Provider> findByIsVerifiedAndIsAvailable(
            boolean isVerified,
            boolean isAvailable
    );
}


//**Why each method exists:**
//findByUserId()
//→ doctor logs in → we find their profile
//→ also checks if profile already exists before creating new one
//
//findBySpecialization()  
//→ patient selects "Cardiologist" from dropdown  
//→ we return all cardiologists
//
//findByIsVerified()
//→ admin wants to see all pending verifications 
//→ patient search only shows verified doctors
//
//searchByNameOrSpecialization()
//→ search bar on patient dashboard
//→ custom @Query needed because name is in users table
//→ LOWER() makes it case insensitive
//
//findByIsVerifiedAndIsAvailable()
//→ most important query for patient search
//→ both conditions must be true
//→ verified by admin AND currently available