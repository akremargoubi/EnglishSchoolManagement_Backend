package com.englishschool.enrollmentservice.repository;

import com.englishschool.enrollmentservice.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    Optional<IdempotencyKey> findByKeyHash(String keyHash);
}
