package com.chiangraipass.privacy.service;

import java.util.List;
import java.util.UUID;

import com.chiangraipass.config.AuthProperties;
import com.chiangraipass.privacy.dto.PrivacyPreferencesRequest;
import com.chiangraipass.privacy.dto.PrivacyPreferencesRequest.Choice;
import com.chiangraipass.privacy.dto.PrivacyPreferencesResponse;
import com.chiangraipass.privacy.model.PrivacyPurpose;
import com.chiangraipass.privacy.repository.PrivacyPreferenceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PrivacyPreferenceServiceTest {

    private static final String VERSION = "2026-10";

    @Autowired
    private PrivacyPreferenceRepository repository;

    private final UUID account = UUID.randomUUID();

    private PrivacyPreferenceService service(String version, String url) {
        return new PrivacyPreferenceService(repository,
                new AuthProperties(new AuthProperties.Auth("http://localhost:5500", true),
                        new AuthProperties.Privacy(version, url), null));
    }

    private PrivacyPreferenceService published() {
        return service(VERSION, "https://example.com/privacy");
    }

    private PrivacyPreferencesRequest choose(String version, PrivacyPurpose purpose, boolean granted) {
        return new PrivacyPreferencesRequest(version, List.of(new Choice(purpose, granted)));
    }

    private boolean granted(PrivacyPreferencesResponse response, PrivacyPurpose purpose) {
        return response.preferences().stream()
                .filter(item -> item.purpose() == purpose).findFirst().orElseThrow().granted();
    }

    @Test
    void everythingStartsAsNotGranted() {
        PrivacyPreferencesResponse response = published().view(account);

        assertThat(response.available()).isTrue();
        assertThat(response.noticeVersion()).isEqualTo(VERSION);
        assertThat(response.preferences()).hasSize(PrivacyPurpose.values().length);
        assertThat(response.preferences()).noneMatch(item -> item.granted() || item.recordedAt() != null);
    }

    @Test
    void grantingThenWithdrawingKeepsBothRecords() {
        PrivacyPreferenceService service = published();

        assertThat(granted(service.save(account, choose(VERSION, PrivacyPurpose.MARKETING_EMAILS, true)),
                PrivacyPurpose.MARKETING_EMAILS)).isTrue();
        assertThat(granted(service.save(account, choose(VERSION, PrivacyPurpose.MARKETING_EMAILS, false)),
                PrivacyPurpose.MARKETING_EMAILS)).isFalse();

        assertThat(repository.findByAccountIdOrderByIdDesc(account))
                .extracting("granted", "noticeVersion")
                .containsExactly(tuple(false, VERSION), tuple(true, VERSION));
    }

    @Test
    void savingAnUnchangedChoiceAddsNothing() {
        PrivacyPreferenceService service = published();
        service.save(account, choose(VERSION, PrivacyPurpose.USAGE_ANALYTICS, true));
        service.save(account, choose(VERSION, PrivacyPurpose.USAGE_ANALYTICS, true));
        service.save(account, choose(VERSION, PrivacyPurpose.MARKETING_EMAILS, false));

        assertThat(repository.findByAccountIdOrderByIdDesc(account)).hasSize(1);
    }

    @Test
    void choicesAreKeptPerAccount() {
        PrivacyPreferenceService service = published();
        service.save(account, choose(VERSION, PrivacyPurpose.MARKETING_EMAILS, true));

        assertThat(granted(service.view(UUID.randomUUID()), PrivacyPurpose.MARKETING_EMAILS)).isFalse();
    }

    @Test
    void refusesAStaleNoticeVersion() {
        assertThatThrownBy(() -> published().save(account, choose("old-version", PrivacyPurpose.MARKETING_EMAILS, true)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("privacy notice has changed");
        assertThat(repository.findByAccountIdOrderByIdDesc(account)).isEmpty();
    }

    @Test
    void refusesToRecordWhileNoNoticeIsPublished() {
        PrivacyPreferenceService unpublished = service("unpublished", "");

        assertThat(unpublished.view(account).available()).isFalse();
        assertThatThrownBy(() -> unpublished.save(account, choose("unpublished", PrivacyPurpose.MARKETING_EMAILS, true)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no privacy notice has been published");
    }
}