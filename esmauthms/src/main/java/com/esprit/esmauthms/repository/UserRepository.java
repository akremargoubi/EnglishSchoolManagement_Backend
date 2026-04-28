package com.esprit.esmauthms.repository;

import com.esprit.esmauthms.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Page<User> findByEmailContainingIgnoreCaseAndFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCaseAndCinContainingAndPhoneNumberContaining(
            String email,
            String firstName,
            String lastName,
            String cin,
            String phoneNumber,
            Pageable pageable
    );

    // Eagerly fetches studentClass in a single LEFT JOIN to avoid N+1 lazy loads in toDto
    @EntityGraph(attributePaths = {"studentClass"})
    Page<User> findByRoleAndDeletedAtIsNull(String role, Pageable pageable);

    List<User> findByParentId(UUID parentId);

    Optional<User> findByEmailAndRole(String email, String role);
}
