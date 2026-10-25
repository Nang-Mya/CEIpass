package com.chiangraipass.privacy.service;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.chiangraipass.config.AuthProperties;
import com.chiangraipass.privacy.dto.PrivacyPreferencesRequest;
import com.chiangraipass.privacy.dto.PrivacyPreferencesResponse;
import com.chiangraipass.privacy.dto.PrivacyPreferencesResponse.Item;
import com.chiangraipass.privacy.entity.PrivacyPreferenceEntity;
import com.chiangraipass.privacy.model.PrivacyPurpose;
import com.chiangraipass.privacy.repository.PrivacyPreferenceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PrivacyPreferenceService {

    private final PrivacyPreferenceRepository repository;
    private final AuthProperties authProperties;

    public PrivacyPreferenceService(PrivacyPreferenceRepository repository, AuthProperties authProperties) {
        this.repository = repository;
        this.authProperties = authProperties;
    }

    @Transactional(readOnly = true)
    public PrivacyPreferencesResponse view(UUID accountId) {
        AuthProperties.Privacy privacy = authProperties.privacy();
        boolean published = privacy.isNoticePublished();
        return new PrivacyPreferencesResponse(
                published,
                published ? privacy.noticeVersion() : null,
                published ? privacy.noticeUrl() : null,
                current(accountId));
    }

    /** Appends a row for every purpose whose choice differs from the current one. */
    @Transactional
    public PrivacyPreferencesResponse save(UUID accountId, PrivacyPreferencesRequest request) {
        AuthProperties.Privacy privacy = authProperties.privacy();
        if (!privacy.isNoticePublished()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Privacy choices are not available yet because no privacy notice has been published.");
        }
        if (!privacy.noticeVersion().equals(request.noticeVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The privacy notice has changed. Reload the page and review it before saving.");
        }
        Map<PrivacyPurpose, Boolean> choices = new EnumMap<>(PrivacyPurpose.class);
        request.choices().forEach(choice -> choices.put(choice.purpose(), choice.granted()));

        Map<PrivacyPurpose, PrivacyPreferenceEntity> latest = latestByPurpose(accountId);
        Instant now = Instant.now();
        choices.forEach((purpose, granted) -> {
            PrivacyPreferenceEntity existing = latest.get(purpose);
            boolean currently = existing != null && existing.isGranted();
            if (currently != granted) {
                repository.save(new PrivacyPreferenceEntity(
                        accountId, purpose, granted, privacy.noticeVersion(), now));
            }
        });
        return view(accountId);
    }

    private List<Item> current(UUID accountId) {
        Map<PrivacyPurpose, PrivacyPreferenceEntity> latest = latestByPurpose(accountId);
        return Arrays.stream(PrivacyPurpose.values())
                .map(purpose -> {
                    PrivacyPreferenceEntity row = latest.get(purpose);
                    return new Item(
                            purpose,
                            row != null && row.isGranted(),
                            row == null ? null : row.getRecordedAt());
                })
                .toList();
    }

    private Map<PrivacyPurpose, PrivacyPreferenceEntity> latestByPurpose(UUID accountId) {
        Map<PrivacyPurpose, PrivacyPreferenceEntity> latest = new EnumMap<>(PrivacyPurpose.class);
        for (PrivacyPreferenceEntity row : repository.findByAccountIdOrderByIdDesc(accountId)) {
            latest.putIfAbsent(row.getPurpose(), row);
        }
        return latest;
    }
}