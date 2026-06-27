package com.legalaid.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    // Used in AuthService - find by Google id on login
    Optional<User> findByGoogleId(String googleId);

    // User in auth service fallback lookup by email
    Optional<User> findByEmail(String email);

    // Soft delete aware - excludes deleted users
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findActiveById(UUID id);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findActiveByEmail(String email);

    // Check if email already taken (for validation)
    Boolean existsByEmail(String email);
}
