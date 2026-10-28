package com.chiangraipass.history.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.sql.init.mode=never")
@Transactional
class PassSummaryControllerTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");
    private static final Set<String> PASS_KEYS =
            Set.of("passName", "status", "startDate", "endDate", "endsAt", "displayStatus", "startsAt");
    private static final Set<String> BENEFIT_KEYS =
            Set.of("attractionName", "detail", "used", "limit");
    private static final Set<String> USAGE_KEYS =
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

    @Test
    void unauthenticatedRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/me/pass-summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedVisitorWithNoPassesReceivesEmptySummary() throws Exception {
        VisitorFixture visitor = createVisitor("empty");

        JsonNode response = getSummary(visitor);

        assertTrueNull(response.get("pass"));
        assertEquals(0, response.get("benefits").size());
        assertEquals(0, response.get("recentUsage").size());
    }

    @Test
    void activePassReturnsBenefitQuotasAndRecentUsageWithOnlyExpectedKeys() throws Exception {
        VisitorFixture visitor = createVisitor("active");
        LocalDate today = LocalDate.now(ZONE);
        PassFixture pass = createPass(visitor, "Active Sample Pass", "ACTIVE", today.minusDays(1), today.plusDays(1),
                Instant.now());
        BenefitEntity benefitOne = createBenefit(pass, "Attraction One", "Benefit One", 1);
        BenefitEntity benefitTwo = createBenefit(pass, "Attraction Two", "Benefit Two", 2);

        createUsage(pass.userPass(), pass.attractions().get(0), benefitOne,
                "VALID_SUCCESS", Instant.parse("2026-01-01T00:00:00Z"));
        createUsage(pass.userPass(), pass.attractions().get(0), benefitOne,
                "QUOTA_EXCEEDED", Instant.parse("2026-01-02T00:00:00Z"));
        createUsage(pass.userPass(), pass.attractions().get(1), benefitTwo,
                "VALID_SUCCESS", Instant.parse("2026-01-03T00:00:00Z"));
        usageLogRepository.flush();

        JsonNode response = getSummary(visitor);
        JsonNode passInfo = response.get("pass");
        assertEquals("Active Sample Pass", passInfo.get("passName").asText());
        assertEquals("ACTIVE", passInfo.get("status").asText());
        assertEquals("ACTIVE", passInfo.get("displayStatus").asText());
        assertEquals(today.minusDays(1).toString(), passInfo.get("startDate").asText());
        assertEquals(today.plusDays(1).toString(), passInfo.get("endDate").asText());
        assertEquals(today.plusDays(1).atTime(23, 59, 59).atZone(ZONE).toInstant().toString(),
                passInfo.get("endsAt").asText());
        assertEquals(today.minusDays(1).atStartOfDay(ZONE).toInstant().toString(),
                passInfo.get("startsAt").asText());
        assertEquals(2, response.get("benefits").size());
        JsonNode benefitOneResult = benefitForAttraction(response.get("benefits"), "Attraction One");
        assertEquals(1, benefitOneResult.get("used").asInt());
        assertEquals(1, benefitOneResult.get("limit").asInt());
        JsonNode benefitTwoResult = benefitForAttraction(response.get("benefits"), "Attraction Two");
        assertEquals(1, benefitTwoResult.get("used").asInt());
        assertEquals(2, benefitTwoResult.get("limit").asInt());

        JsonNode recentUsage = response.get("recentUsage");
        assertEquals(3, recentUsage.size());
        assertEquals("VALID_SUCCESS", recentUsage.get(0).get("status").asText());
        assertEquals("QUOTA_EXCEEDED", recentUsage.get(1).get("status").asText());
        assertEquals("VALID_SUCCESS", recentUsage.get(2).get("status").asText());
        assertEquals(Instant.parse("2026-01-03T00:00:00Z").toString(),
                recentUsage.get(0).get("usedAt").asText());

        assertOnlyKeys(passInfo, PASS_KEYS);
        for (JsonNode benefit : response.get("benefits")) {
            assertOnlyKeys(benefit, BENEFIT_KEYS);
        }
        for (JsonNode usage : recentUsage) {
            assertOnlyKeys(usage, USAGE_KEYS);
        }
    }

    @Test
    void activePassWithEndDateYesterdayIsReportedExpired() throws Exception {
        VisitorFixture visitor = createVisitor("expired");
        LocalDate today = LocalDate.now(ZONE);
        createPass(visitor, "Expired Sample Pass", "ACTIVE", today.minusDays(3), today.minusDays(1), Instant.now());

        JsonNode response = getSummary(visitor);

        assertEquals("EXPIRED", response.get("pass").get("status").asText());
        assertEquals("EXPIRED", response.get("pass").get("displayStatus").asText());
    }

    @Test
    void futurePassIsNotStartedUntilItsStartDate() throws Exception {
        VisitorFixture visitor = createVisitor("future");
        LocalDate today = LocalDate.now(ZONE);
        createPass(visitor, "Future Sample Pass", "ACTIVE", today.plusDays(1), today.plusDays(2), Instant.now());

        JsonNode response = getSummary(visitor);
        JsonNode passInfo = response.get("pass");

        assertEquals("ACTIVE", passInfo.get("status").asText());
        assertEquals("NOT STARTED", passInfo.get("displayStatus").asText());
        assertEquals(today.plusDays(1).atStartOfDay(ZONE).toInstant().toString(),
                passInfo.get("startsAt").asText());
    }

    @Test
    void passWithMissingDatesIsNotStarted() throws Exception {
        VisitorFixture visitor = createVisitor("missing-dates");
        createPass(visitor, "Undated Sample Pass", "UNACTIVATED", null, null, Instant.now());

        JsonNode passInfo = getSummary(visitor).get("pass");

        assertEquals("NOT STARTED", passInfo.get("displayStatus").asText());
        org.junit.jupiter.api.Assertions.assertTrue(passInfo.get("startsAt").isNull());
        org.junit.jupiter.api.Assertions.assertTrue(passInfo.get("endsAt").isNull());
    }

    @Test
    void activePassIsPreferredOverNewerUnactivatedPass() throws Exception {
        VisitorFixture visitor = createVisitor("preference");
        LocalDate today = LocalDate.now(ZONE);
        PassFixture active = createPass(
                visitor, "Active Pass", "ACTIVE", today.minusDays(1), today.plusDays(1), Instant.now().minusSeconds(60));
        createPass(visitor, "Newer Unactivated Pass", "UNACTIVATED", null, null, Instant.now());

        JsonNode response = getSummary(visitor);

        assertEquals("Active Pass", response.get("pass").get("passName").asText());
        assertEquals("ACTIVE", response.get("pass").get("status").asText());
        assertNotNull(active.userPass());
    }

    @Test
    void visitorCannotSeeAnotherVisitorsPassOrUsage() throws Exception {
        VisitorFixture visitorA = createVisitor("owner");
        VisitorFixture visitorB = createVisitor("other");
        LocalDate today = LocalDate.now(ZONE);
        PassFixture passA = createPass(
                visitorA, "Private Visitor A Pass", "ACTIVE", today.minusDays(1), today.plusDays(1), Instant.now());
        BenefitEntity benefit = createBenefit(passA, "Private Attraction A", "Private Benefit A", 1);
        createUsage(passA.userPass(), passA.attractions().get(0), benefit,
                "VALID_SUCCESS", Instant.parse("2026-01-03T00:00:00Z"));
        usageLogRepository.flush();

        JsonNode response = getSummary(visitorB);

        assertTrueNull(response.get("pass"));
        assertEquals(0, response.get("benefits").size());
        assertEquals(0, response.get("recentUsage").size());
    }

    private JsonNode getSummary(VisitorFixture visitor) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/me/pass-summary")
                        .with(visitorAuthentication(visitor.accountId(), visitor.email())))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode benefitForAttraction(JsonNode benefits, String attractionName) {
        for (JsonNode benefit : benefits) {
            if (attractionName.equals(benefit.get("attractionName").asText())) {
                return benefit;
            }
        }
        throw new AssertionError("Benefit for " + attractionName + " was not returned");
    }

    private VisitorFixture createVisitor(String label) {
        UUID accountId = UUID.randomUUID();
        String email = label + "-" + accountId + "@example.test";
        VisitorAccountEntity account = VisitorAccountEntity.newEmailAccount(
                accountId, email, label, "test-password-hash", "test-notice");
        account = accountRepository.saveAndFlush(account);
        return new VisitorFixture(account, accountId, email);
    }

    private PassFixture createPass(
            VisitorFixture visitor,
            String passName,
            String userPassStatus,
            LocalDate startDate,
            LocalDate endDate,
            Instant purchaseDatetime) {
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

        PaymentEntity payment = new PaymentEntity();
        payment.setAccount(visitor.account());
        payment.setPass(pass);
        payment.setPaymentMethod("PROMPTPAY");
        payment.setPaymentStatus("SUCCESS");
        payment.setPaymentDatetime(purchaseDatetime);
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        payment.setAmount(new BigDecimal("10.00"));
        payment = paymentRepository.saveAndFlush(payment);

        UserPassEntity userPass = new UserPassEntity();
        userPass.setAccount(visitor.account());
        userPass.setPass(pass);
        userPass.setPayment(payment);
        userPass.setPurchaseDatetime(purchaseDatetime);
        userPass.setStartDate(startDate);
        userPass.setEndDate(endDate);
        userPass.setStatus(userPassStatus);
        userPass.setCreatedAt(now);
        userPass.setUpdatedAt(now);
        userPass = userPassRepository.saveAndFlush(userPass);
        return new PassFixture(pass, userPass, visitor);
    }

    private BenefitEntity createBenefit(PassFixture pass, String attractionName, String detail, int limit) {
        String suffix = UUID.randomUUID().toString();
        Instant now = Instant.now();

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

        BenefitEntity benefit = new BenefitEntity();
        benefit.setBenefitId("benefit-" + suffix);
        benefit.setPartner(attraction);
        benefit.setPass(pass.pass());
        benefit.setBenefitDetail(detail);
        benefit.setDiscountRate(new BigDecimal("0.00"));
        benefit.setUsageRuleLimit(limit);
        benefit.setCreatedAt(now);
        benefit.setUpdatedAt(now);
        benefit = benefitRepository.saveAndFlush(benefit);
        pass.attractions().add(attraction);
        return benefit;
    }

    private void createUsage(
            UserPassEntity userPass,
            AttractionCatalogEntity attraction,
            BenefitEntity benefit,
            String status,
            Instant checkinDatetime) {
        BenefitUsageLogEntity usage = new BenefitUsageLogEntity();
        usage.setUserPass(userPass);
        usage.setPartner(attraction);
        usage.setBenefit(benefit);
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

    private static void assertOnlyKeys(JsonNode item, Set<String> expectedKeys) {
        Set<String> actualKeys = new HashSet<>();
        item.fieldNames().forEachRemaining(actualKeys::add);
        assertEquals(expectedKeys, actualKeys);
    }

    private static void assertTrueNull(JsonNode value) {
        org.junit.jupiter.api.Assertions.assertTrue(value == null || value.isNull());
    }

    private record VisitorFixture(VisitorAccountEntity account, UUID accountId, String email) {
    }

    private record PassFixture(
            PassCatalogEntity pass,
            UserPassEntity userPass,
            VisitorFixture visitor,
            List<AttractionCatalogEntity> attractions) {
        private PassFixture(PassCatalogEntity pass, UserPassEntity userPass, VisitorFixture visitor) {
            this(pass, userPass, visitor, new ArrayList<>());
        }
    }
}
