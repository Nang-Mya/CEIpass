package com.chiangraipass.pass.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import com.chiangraipass.catalog.entity.PassCatalogEntity;
import com.chiangraipass.pass.dto.QrPassResponse;
import com.chiangraipass.pass.entity.UserPassEntity;
import com.chiangraipass.pass.repository.UserPassJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PassQrServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");

    @Mock
    private UserPassJpaRepository userPassRepository;

    @InjectMocks
    private PassQrService passQrService;

    @Test
    void issuesQrForRequestedActivePass() {
        UUID accountId = UUID.randomUUID();
        UUID firstPassId = UUID.randomUUID();
        UUID requestedPassId = UUID.randomUUID();
        UserPassEntity firstPass = activePass(firstPassId, "First pass");
        UserPassEntity requestedPass = activePass(requestedPassId, "Requested pass");
        when(userPassRepository.findByAccountIdWithPass(accountId))
                .thenReturn(List.of(firstPass, requestedPass));

        QrPassResponse response = passQrService.issueQrForAccount(accountId, requestedPassId);

        assertEquals(requestedPassId, response.userPassId());
        assertEquals("Requested pass", response.passName());
        assertTrue(response.qrImageDataUrl().startsWith("data:image/png;base64,"));
    }

    @Test
    void doesNotIssueQrForPassThatIsNotActiveToday() {
        UUID accountId = UUID.randomUUID();
        UUID futurePassId = UUID.randomUUID();
        UserPassEntity futurePass = activePass(futurePassId, "Future pass");
        futurePass.setStartDate(LocalDate.now(ZONE).plusDays(1));
        when(userPassRepository.findByAccountIdWithPass(accountId)).thenReturn(List.of(futurePass));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> passQrService.issueQrForAccount(accountId, futurePassId));

        assertEquals(409, error.getStatusCode().value());
    }

    private UserPassEntity activePass(UUID userPassId, String passName) {
        PassCatalogEntity catalogPass = new PassCatalogEntity();
        catalogPass.setDisplayName(passName);

        UserPassEntity userPass = new UserPassEntity();
        userPass.setUserPassId(userPassId);
        userPass.setPass(catalogPass);
        userPass.setStatus("ACTIVE");
        userPass.setStartDate(LocalDate.now(ZONE));
        userPass.setEndDate(LocalDate.now(ZONE));
        return userPass;
    }
}
