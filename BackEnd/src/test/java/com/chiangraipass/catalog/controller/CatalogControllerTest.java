package com.chiangraipass.catalog.controller;

import java.math.BigDecimal;
import java.util.List;

import com.chiangraipass.auth.service.VisitorOAuth2UserService;
import com.chiangraipass.catalog.dto.AttractionCatalogItem;
import com.chiangraipass.catalog.dto.PassCatalogItem;
import com.chiangraipass.catalog.service.CatalogService;
import com.chiangraipass.config.CorsConfiguration;
import com.chiangraipass.config.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CatalogController.class)
@Import({CorsConfiguration.class, SecurityConfiguration.class})
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogService catalogService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private VisitorOAuth2UserService oauthUserService;

    @Test
    void returnsAvailablePassesWithDecimalPrices() throws Exception {
        given(catalogService.findPublishedPasses()).willReturn(List.of(
                new PassCatalogItem(
                        "day-pass",
                        "Day Pass",
                        "Valid for one day",
                        new BigDecimal("299.00"),
                        "THB",
                        "1 day",
                        false)));

        mockMvc.perform(get("/api/v1/catalog/passes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("day-pass"))
                .andExpect(jsonPath("$[0].price").value(299.00))
                .andExpect(jsonPath("$[0].currency").value("THB"))
                .andExpect(jsonPath("$[0].available").value(false));
    }

    @Test
    void returnsPublishedAttractionsWithPartnerEligibility() throws Exception {
        given(catalogService.findPublishedAttractions()).willReturn(List.of(
                new AttractionCatalogItem(
                        "partner-1",
                        "Published Partner",
                        "A local museum",
                        "Chiang Rai",
                        "Museum",
                        null,
                        null,
                        true)));

        mockMvc.perform(get("/api/v1/catalog/attractions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].partnerId").value("partner-1"))
                .andExpect(jsonPath("$[0].name").value("Published Partner"))
                .andExpect(jsonPath("$[0].description").value("A local museum"))
                .andExpect(jsonPath("$[0].eligibleForPass").value(true));
    }
}
