package com.medibook.auth.repository;

import com.medibook.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    //  findByEmail()
    Optional<User> findByEmail(String email);

    //  findByUserId()
    Optional<User> findByUserId(int userId);

    //  existsByEmail()
    boolean existsByEmail(String email);

    //  findAllByRole()
    List<User> findAllByRole(String role);

    //  findByPhone()
    Optional<User> findByPhone(String phone);

    //  findByFullNameContaining()
    List<User> findByFullNameContaining(String name);

    //  deleteByUserId()
    void deleteByUserId(int userId);
}


//**Why each method — directly from PDF section 4.1:**
//```
//findByEmail()             → login, check duplicate email on register
//findByUserId()            → get user profile by ID
//existsByEmail()           → check if email already registered
//findAllByRole()           → admin gets all patients or all providers
//findByPhone()             → find user by phone number
//findByFullNameContaining()→ search users by name (admin feature)
//deleteByUserId()          → admin deletes user account