package com.agrimandi.api.controller;

import com.agrimandi.api.entity.Buyer;
import com.agrimandi.api.entity.Farmer;
import com.agrimandi.api.entity.Market;
import com.agrimandi.api.entity.User;
import com.agrimandi.api.repository.BuyerRepository;
import com.agrimandi.api.repository.FarmerRepository;
import com.agrimandi.api.repository.MarketRepository;
import com.agrimandi.api.repository.UserRepository;
import com.agrimandi.api.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Server-side authorization for the reporting and audit endpoints.
 *
 * <p>This asserts that hiding routes in the UI is never the only protection: FARMER and BUYER
 * are rejected by the API itself, and audit access is ADMIN-only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportingAccessControlTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MarketRepository marketRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FarmerRepository farmerRepository;
    @Autowired private BuyerRepository buyerRepository;

    private User agent;
    private User admin;
    private User farmerUser;
    private User buyerUser;

    @BeforeEach
    void setUp() {
        Market market = new Market();
        market.setName("Pune APMC");
        market.setCode("PUN01");
        market.setStatus("ACTIVE");
        market = marketRepository.save(market);

        agent = user("agent.ac", "AGENT", market, null, null);
        admin = user("secretary.ac", "ADMIN", market, null, null);

        Farmer farmer = new Farmer();
        farmer.setName("Ramesh Patil");
        farmer.setMarket(market);
        farmer.setStatus("ACTIVE");
        farmer = farmerRepository.save(farmer);
        farmerUser = user("farmer.ac", "FARMER", market, farmer, null);

        Buyer buyer = new Buyer();
        buyer.setName("Buyer B");
        buyer.setMarket(market);
        buyer.setStatus("ACTIVE");
        buyer = buyerRepository.save(buyer);
        buyerUser = user("buyer.ac", "BUYER", market, null, buyer);
    }

    @Test
    @DisplayName("An agent can read operational reports for their own market")
    void agent_canAccessReports() throws Exception {
        mockMvc.perform(get("/api/reports/transactions").with(as(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aggregate.totalTransactions").exists());
        mockMvc.perform(get("/api/reports/payments").with(as(agent))).andExpect(status().isOk());
        mockMvc.perform(get("/api/reports/assets").with(as(agent))).andExpect(status().isOk());
        mockMvc.perform(get("/api/reports/auctions").with(as(agent))).andExpect(status().isOk());
        mockMvc.perform(get("/api/reports/buyers").with(as(agent))).andExpect(status().isOk());
        mockMvc.perform(get("/api/reports/farmers").with(as(agent))).andExpect(status().isOk());
        mockMvc.perform(get("/api/reports/overview").with(as(agent))).andExpect(status().isOk());
        mockMvc.perform(get("/api/reports/payment-ledger").with(as(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
        mockMvc.perform(get("/api/reports/asset-ledger").with(as(agent))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("An admin can read operational reports and the audit trail")
    void admin_canAccessReportsAndAudit() throws Exception {
        mockMvc.perform(get("/api/reports/transactions").with(as(admin))).andExpect(status().isOk());
        mockMvc.perform(get("/api/reports/overview").with(as(admin))).andExpect(status().isOk());
        mockMvc.perform(get("/api/audit-logs").with(as(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").exists());
        mockMvc.perform(get("/api/audit-logs/actions").with(as(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("An agent cannot read the audit trail")
    void agent_cannotAccessAudit() throws Exception {
        mockMvc.perform(get("/api/audit-logs").with(as(agent))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/audit-logs/actions").with(as(agent))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A farmer cannot read market-wide reporting or the audit trail")
    void farmer_cannotAccessReports() throws Exception {
        mockMvc.perform(get("/api/reports/transactions").with(as(farmerUser))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/payments").with(as(farmerUser))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/assets").with(as(farmerUser))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/auctions").with(as(farmerUser))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/buyers").with(as(farmerUser))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/farmers").with(as(farmerUser))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/overview").with(as(farmerUser))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/payment-ledger").with(as(farmerUser))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/asset-ledger").with(as(farmerUser))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/audit-logs").with(as(farmerUser))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A buyer cannot read market-wide reporting or the audit trail")
    void buyer_cannotAccessReports() throws Exception {
        mockMvc.perform(get("/api/reports/transactions").with(as(buyerUser))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/payments").with(as(buyerUser))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/buyers").with(as(buyerUser))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/overview").with(as(buyerUser))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/payment-ledger").with(as(buyerUser))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/asset-ledger").with(as(buyerUser))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/audit-logs").with(as(buyerUser))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated requests are rejected")
    void anonymous_isRejected() throws Exception {
        mockMvc.perform(get("/api/reports/transactions")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/reports/overview")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/audit-logs")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Date filtering is driven by request parameters, not by the client clock")
    void dateFiltering_isServerDriven() throws Exception {
        mockMvc.perform(get("/api/reports/transactions").param("period", "YESTERDAY").with(as(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.range.from").exists())
                .andExpect(jsonPath("$.range.to").exists());

        mockMvc.perform(get("/api/reports/transactions").param("period", "CUSTOM").with(as(agent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Explicit from/to dates are honoured without also sending period=CUSTOM")
    void dateFiltering_explicitDatesWorkWithoutPeriodParameter() throws Exception {
        mockMvc.perform(get("/api/reports/transactions")
                        .param("from", "2099-01-01")
                        .param("to", "2099-12-31")
                        .with(as(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.range.from").value(org.hamcrest.Matchers.startsWith("2099-01-01")));

        mockMvc.perform(get("/api/reports/payment-ledger")
                        .param("from", "2099-01-01")
                        .param("to", "2099-12-31")
                        .with(as(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/reports/asset-ledger")
                        .param("from", "2099-01-01")
                        .param("to", "2099-12-31")
                        .with(as(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Omitting the period still falls back to the current month")
    void dateFiltering_absentPeriodDefaultsToCurrentMonth() throws Exception {
        mockMvc.perform(get("/api/reports/transactions").with(as(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.range.from").exists());
    }

    private RequestPostProcessor as(User user) {
        CustomUserDetails details = new CustomUserDetails(user);
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        return authentication(authentication);
    }

    private User user(String username, String role, Market market, Farmer farmer, Buyer buyer) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("test-hash");
        user.setRole(role);
        user.setStatus("ACTIVE");
        user.setMarket(market);
        user.setFarmer(farmer);
        user.setBuyer(buyer);
        return userRepository.save(user);
    }
}
