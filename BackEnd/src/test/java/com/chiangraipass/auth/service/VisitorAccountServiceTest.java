package com.chiangraipass.auth.service;

import java.time.Instant;
import java.util.UUID;

import com.chiangraipass.auth.entity.VisitorAccountEntity;
import com.chiangraipass.auth.repository.VisitorAccountJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VisitorAccountServiceTest {

    @Autowired
    private VisitorAccountJpaRepository accounts;

    private VisitorAccountService service;

    @BeforeEach
    void setUp() {
        service = new VisitorAccountService(accounts);
    }

    private VisitorAccountEntity saveAccount() {
        return accounts.saveAndFlush(VisitorAccountEntity.newEmailAccount(
                UUID.randomUUID(), "a@example.com", "Alice", "old-hash", "v1"));
    }

    @Test
    void findsTheSecurityTokenOfAnActiveAccount() {
        VisitorAccountEntity account = saveAccount();

        assertThat(service.findSecurityToken(account.getAccountId())).contains(account.getSecurityToken());
        assertThat(service.findSecurityToken(UUID.randomUUID())).isEmpty();
    }

    @Test
    void changingThePasswordRotatesTheTokenAndUnlocks() {
        VisitorAccountEntity account = saveAccount();
        String oldToken = account.getSecurityToken();
        for (int i = 0; i < 5; i++) {
            account.recordFailedLogin(Instant.now(), 5, java.time.Duration.ofMinutes(10));
        }

        boolean changed = service.changePassword(account.getAccountId(), "new-hash");

        assertThat(changed).isTrue();
        assertThat(account.getPasswordHash()).isEqualTo("new-hash");
        assertThat(account.getSecurityToken()).isNotEqualTo(oldToken);
        assertThat(service.findSecurityToken(account.getAccountId())).contains(account.getSecurityToken());
        assertThat(account.isLocked(Instant.now())).isFalse();
        assertThat(account.getFailedLoginCount()).isZero();
    }

    @Test
    void changingThePasswordOfAnUnknownAccountDoesNothing() {
        assertThat(service.changePassword(UUID.randomUUID(), "new-hash")).isFalse();
    }

    @Test
    void updatesTheDisplayNameOfAnActiveAccount() {
        VisitorAccountEntity account = saveAccount();

        assertThat(service.updateDisplayName(account.getAccountId(), "New Name")).isTrue();

        assertThat(service.findById(account.getAccountId()))
                .hasValueSatisfying(found -> {
                    assertThat(found.displayName()).isEqualTo("New Name");
                    assertThat(found.passwordHash()).isNotNull();
                });
        assertThat(service.updateDisplayName(UUID.randomUUID(), "Nobody")).isFalse();
        assertThat(service.findById(UUID.randomUUID())).isEmpty();
    }
    
}