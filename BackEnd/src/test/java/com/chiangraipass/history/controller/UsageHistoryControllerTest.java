package com.chiangraipass.history.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.chiangraipass.auth.entity.VisitorAccountEntity;
import com.chiangraipass.auth.model.EmailVisitorPrincipal;
import com.chiangraipass.auth.model.VisitorAccount;
import com.chiangraipass.auth.repository.VisitorAccountJpaRepository;
import com.chiangraipass.catalog.entity.AttractionCatalogEntity;
import com.chiangraipass.catalog.entity.PassCatalogEntity;
import com.chiangraipass.catalog.repository.AttractionCatalogJpaRepository;
import com.chiangraipass.catalog.repository.PassCatalogJpaRepository;
import com.chiangraipass.pass.entity.BenefitEntity;
import com.chiangraipass.pass.entity.BenefitUsageLogEntity;
import com.chiangraipass.pass.entity.UserPassEntity;
import com.chiangraipass.pass.repository.BenefitJpaRepository;
import com.chiangraipass.pass.repository.BenefitUsageLogJpaRepository;
import com.chiangraipass.pass.repository.UserPassJpaRepository;
import com.chiangraipass.payment.entity.PaymentEntity;
import com.chiangraipass.payment.repository.PaymentJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.sql.init.mode=never")
@Transactional
class UsageHistoryControllerTest {

    private static final Set<String> EXPECTED_ITEM_KEYS =
            Set.of("attractionName", "passName", "usedAt", "status");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VisitorAccountJpaRepository accountRepository;

    @Autowired
    private PassCatalogJpaRepository passRepository;

    @Autowired
    private AttractionCatalogJpaRepository attractionRepository;

    @Autowired
    private PaymentJpaRepository paymentRepository;

    @Autowired
    private UserPassJpaRepository userPassRepository;

    @Autowired
    private BenefitJpaRepository benefitRepository;

    @Autowired
    private BenefitUsageLogJpaRepository usageLogRepository;

    private VisitorFixture visitorA;
    private VisitorFixture visitorB;
    private Instant newestUsageTime;

    @BeforeEach
    void setUpUsageHistory() {
        visitorA = createVisitorFixture("visitor-a", "Pass A", "Attraction A");
        visitorB = createVisitorFixture("visitor-b", "Pass B", "Attraction B");

        newestUsageTime = Instant.parse("2026-01-03T12:00:00Z");
        createUsage(visitorA, Instant.parse("2026-01-01T12:00:00Z"), "VALID_SUCCESS");
        createUsage(visitorA, Instant.parse("2026-01-02T12:00:00Z"), "QUOTA_EXCEEDED");
        createUsage(visitorA, newestUsageTime, "VALID_SUCCESS");
        createUsage(visitorB, Instant.parse("2026-01-04T12:00:00Z"), "VALID_SUCCESS");

        usageLogRepository.flush();
    }

    @Test
    void unauthenticatedRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/me/usage-history"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedVisitorWithNoUsageRowsReceivesEmptyArray() throws Exception {
        UUID accountId = UUID.randomUUID();
        MvcResult result = mockMvc.perform(get("/api/v1/me/usage-history")
                        .with(visitorAuthentication(accountId, "empty@example.test")))
                .andExpect(status().isOk())
                .andReturn();

        assertTrue(objectMapper.readTree(result.getResponse().getContentAsString()).isArray());
        assertEquals(0, objectMapper.readTree(result.getResponse().getContentAsString()).size());
    }

    @Test
    void returnsOnlyAuthenticatedVisitorsHistoryNewestFirstWithExpectedFields() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/me/usage-history")
                        .with(visitorAuthentication(visitorA.accountId(), visitorA.email())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(3, items.size());
        assertEquals("Attraction A", items.get(0).get("attractionName").asText());
        assertEquals("Pass A", items.get(0).get("passName").asText());
        assertEquals(newestUsageTime.toString(), items.get(0).get("usedAt").asText());
        assertEquals("VALID_SUCCESS", items.get(0).get("status").asText());
        assertEquals("QUOTA_EXCEEDED", items.get(1).get("status").asText());
        assertEquals("VALID_SUCCESS", items.get(2).get("status").asText());

        for (JsonNode item : items) {
            Set<String> actualKeys = new HashSet<>();
            item.fieldNames().forEachRemaining(actualKeys::add);
            assertEquals(EXPECTED_ITEM_KEYS, actualKeys);
            assertFalse(item.get("attractionName").asText().contains("B"));
            assertFalse(item.get("passName").asText().contains("B"));
        }
    }

    @Test
    void returnsOnlyAuthenticatedVisitorsHistoryForVisitorB() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/me/usage-history")
                        .with(visitorAuthentication(visitorB.accountId(), visitorB.email())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(1, items.size());
        assertEquals("Attraction B", items.get(0).get("attractionName").asText());
        assertEquals("Pass B", items.get(0).get("passName").asText());
    }

    @Test
    void limitOneReturnsNewestUsage() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/me/usage-history")
                        .param("limit", "1")
                        .with(visitorAuthentication(visitorA.accountId(), visitorA.email())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(1, items.size());
        assertEquals(newestUsageTime.toString(), items.get(0).get("usedAt").asText());
        assertEquals("VALID_SUCCESS", items.get(0).get("status").asText());
    }

    @Test
    void userPassIdReturnsOnlyThatPassHistory() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/me/usage-history")
                        .param("userPassId", visitorA.userPass().getUserPassId().toString())
                        .with(visitorAuthentication(visitorA.accountId(), visitorA.email())))
                .andExpect(status().isOk())
                .andReturn();

        var items = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(3, items.size());
        assertEquals("Attraction A", items.get(0).get("attractionName").asText());
        assertEquals("VALID_SUCCESS", items.get(0).get("status").asText());
    }

    @Test
    void userPassIdFromAnotherVisitorReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/me/usage-history")
                        .param("userPassId", visitorB.userPass().getUserPassId().toString())
                        .with(visitorAuthentication(visitorA.accountId(), visitorA.email())))
                .andExpect(status().isNotFound());
    }

    private VisitorFixture createVisitorFixture(String label, String passName, String attractionName) {
        UUID accountId = UUID.randomUUID();
        String email = label + "-" + accountId + "@example.test";
        VisitorAccountEntity account = VisitorAccountEntity.newEmailAccount(
                accountId, email, label, "test-password-hash", "test-notice");
        account = accountRepository.saveAndFlush(account);

        String suffix = UUID.randomUUID().toString();
        Instant now = Instant.now();

        PassCatalogEntity pass = new PassCatalogEntity();
        pass.setPassId("pass-" + suffix);
        pass.setDisplayName(passName);
        pass.setDescription("Test pass");
        pass.setPrice(new BigDecimal("10.00"));
        pass.setCurrency("THB");
        pass.setValidityDescription("Test validity");
        pass.setAvailable(true);
        pass.setPublished(true);
        pass.setDisplayOrder(1);
        pass.setCreatedAt(now);
        pass.setUpdatedAt(now);
        pass = passRepository.saveAndFlush(pass);

        AttractionCatalogEntity attraction = new AttractionCatalogEntity();
        attraction.setPartnerId("partner-" + suffix);
        attraction.setPublicName(attractionName);
        attraction.setDescription("Test attraction");
        attraction.setLocation("Chiang Rai");
        attraction.setCategory("Test");
        attraction.setPassEligible(true);
        attraction.setPublished(true);
        attraction.setDisplayOrder(1);
        attraction.setCreatedAt(now);
        attraction.setUpdatedAt(now);
        attraction = attractionRepository.saveAndFlush(attraction);

        PaymentEntity payment = new PaymentEntity();
        payment.setAccount(account);
        payment.setPass(pass);
        payment.setPaymentMethod("PROMPTPAY");
        payment.setPaymentStatus("SUCCESS");
        payment.setPaymentDatetime(now);
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        payment.setAmount(new BigDecimal("10.00"));
        payment = paymentRepository.saveAndFlush(payment);

        UserPassEntity userPass = new UserPassEntity();
        userPass.setAccount(account);
        userPass.setPass(pass);
        userPass.setPayment(payment);
        userPass.setPurchaseDatetime(now);
        userPass.setStatus("UNACTIVATED");
        userPass.setCreatedAt(now);
        userPass.setUpdatedAt(now);
        userPass = userPassRepository.saveAndFlush(userPass);

        BenefitEntity benefit = new BenefitEntity();
        benefit.setBenefitId("benefit-" + suffix);
        benefit.setPartner(attraction);
        benefit.setPass(pass);
        benefit.setBenefitDetail("Test benefit");
        benefit.setDiscountRate(new BigDecimal("0.00"));
        benefit.setUsageRuleLimit(1);
        benefit.setCreatedAt(now);
        benefit.setUpdatedAt(now);
        benefit = benefitRepository.saveAndFlush(benefit);

        return new VisitorFixture(accountId, email, userPass, attraction, benefit);
    }

    private void createUsage(VisitorFixture visitor, Instant checkinDatetime, String status) {
        BenefitUsageLogEntity usage = new BenefitUsageLogEntity();
        usage.setUserPass(visitor.userPass());
        usage.setPartner(visitor.attraction());
        usage.setBenefit(visitor.benefit());
        usage.setCheckinDatetime(checkinDatetime);
        usage.setValidationStatus(status);
        usageLogRepository.save(usage);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor visitorAuthentication(
            UUID accountId,
            String email) {
        EmailVisitorPrincipal principal = new EmailVisitorPrincipal(
                new VisitorAccount(accountId, email, "Test Visitor", "test-password-hash"));
        UsernamePasswordAuthenticationToken authenticated = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
        return authentication(authenticated);
    }

    private record VisitorFixture(
            UUID accountId,
            String email,
            UserPassEntity userPass,
            AttractionCatalogEntity attraction,
            BenefitEntity benefit) {
    }
}
