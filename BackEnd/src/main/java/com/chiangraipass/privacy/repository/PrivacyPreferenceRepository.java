package com.chiangraipass.privacy.repository;

import java.util.List;
import java.util.UUID;

import com.chiangraipass.privacy.entity.PrivacyPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivacyPreferenceRepository extends JpaRepository<PrivacyPreferenceEntity, Long> {

    /** Newest first, so the first row seen for a purpose is its current choice. */
    List<PrivacyPreferenceEntity> findByAccountIdOrderByIdDesc(UUID accountId);
}