package com.agrimandi.api.controller;

import com.agrimandi.api.entity.Buyer;
import com.agrimandi.api.entity.Commodity;
import com.agrimandi.api.entity.Farmer;
import com.agrimandi.api.entity.Lot;
import com.agrimandi.api.entity.Market;
import com.agrimandi.api.entity.User;
import com.agrimandi.api.repository.BuyerRepository;
import com.agrimandi.api.repository.CommodityRepository;
import com.agrimandi.api.repository.FarmerRepository;
import com.agrimandi.api.repository.LotRepository;
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

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authorization for the participant portals and the per-lot read.
 *
 * <p>These endpoints are the ones the farmer and buyer screens depend on. A portal must only
 * ever return the signed-in participant's own records, and hiding a route in React must never be
 * the only thing standing between one farmer and another farmer's ledger.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortalAccessControlTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MarketRepository marketRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FarmerRepository farmerRepository;
    @Autowired private BuyerRepository buyerRepository;
    @Autowired private CommodityRepository commodityRepository;
    @Autowired private LotRepository lotRepository;

    private User agent;
    private User farmerUser;
    private User otherFarmerUser;
    private User buyerUser;
    private Farmer farmer;
    private Farmer otherFarmer;
    private Lot lot;

    @BeforeEach
    void setUp() {
        Market market = new Market();
        market.setName("Pune APMC");
        market.setCode("PUN01");
        market.setStatus("ACTIVE");
        market = marketRepository.save(market);

        agent = user("agent.portal", "AGENT", market, null, null);

        farmer = farmer("Ramesh Patil", "RAM001", market);
        farmerUser = user("farmer.portal", "FARMER", market, farmer, null);

        Farmer second = farmer("Sunil Jadhav", "SUN001", market);
        otherFarmerUser = user("farmer.other", "FARMER", market, second, null);

        Buyer buyer = new Buyer();
        buyer.setName("Buyer B");
        buyer.setBuyerCode("BUY001");
        buyer.setMarket(market);
        buyer.setStatus("ACTIVE");
        buyer = buyerRepository.save(buyer);
        buyerUser = user("buyer.portal", "BUYER", market, null, buyer);

        Commodity tomato = new Commodity();
        tomato.setName("Tomato");
        tomato.setActiveStatus(true);
        tomato = commodityRepository.save(tomato);

        lot = new Lot();
        lot.setLotNumber("LOT-2026-000001");
        lot.setMarket(market);
        lot.setFarmer(farmer);
        lot.setCommodity(tomato);
        lot.setQuantity(new BigDecimal("500.00"));
        lot.setQuantityUnit("KG");
        lot.setPackagingType("Loose");
        lot.setStatus("READY_FOR_AUCTION");
        lot = lotRepository.save(lot);
    }

    @Test
    @DisplayName("A farmer reads their own profile and their own transactions")
    void farmer_readsOwnPortal() throws Exception {
        mockMvc.perform(get("/api/farmer/me").with(as(farmerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(farmer.getId().toString()))
                .andExpect(jsonPath("$.name").value("Ramesh Patil"));

        mockMvc.perform(get("/api/farmer/me/transactions").with(as(farmerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("The farmer profile endpoint is scoped to the caller, not to a path parameter")
    void farmer_cannotReadAnotherFarmersPortal() throws Exception {
        // The other farmer's own request still resolves to their own record, never to a shared one.
        mockMvc.perform(get("/api/farmer/me").with(as(otherFarmerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(otherFarmerUser.getFarmer().getId().toString()))
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(farmer.getId().toString())));
    }

    @Test
    @DisplayName("A buyer reads their own profile and their own purchases")
    void buyer_readsOwnPortal() throws Exception {
        mockMvc.perform(get("/api/buyer/me").with(as(buyerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Buyer B"));

        mockMvc.perform(get("/api/buyer/me/purchases").with(as(buyerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Portal endpoints reject the wrong role")
    void portals_rejectWrongRole() throws Exception {
        mockMvc.perform(get("/api/farmer/me").with(as(agent))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/farmer/me/transactions").with(as(buyerUser))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/buyer/me").with(as(agent))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/buyer/me/purchases").with(as(farmerUser))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A buyer cannot read a lot they never transacted on")
    void buyer_cannotReadUnrelatedLot() throws Exception {
        // This buyer has no transactions at all, so the market's lots are not theirs to read.
        mockMvc.perform(get("/api/lots/" + lot.getId()).with(as(buyerUser)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("A farmer cannot read another farmer's lot")
    void farmer_cannotReadUnrelatedLot() throws Exception {
        mockMvc.perform(get("/api/lots/" + lot.getId()).with(as(otherFarmerUser)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("The owning farmer and the agent can read the lot")
    void ownerAndAgent_canReadLot() throws Exception {
        mockMvc.perform(get("/api/lots/" + lot.getId()).with(as(farmerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lotNumber").value("LOT-2026-000001"));
        mockMvc.perform(get("/api/lots/" + lot.getId()).with(as(agent))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("An agent resolves their own market instead of a client-supplied one")
    void agent_resolvesOwnMarket() throws Exception {
        mockMvc.perform(get("/api/markets/me").with(as(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PUN01"));
    }

    @Test
    @DisplayName("Unauthenticated requests to the portals are rejected")
    void anonymous_isRejected() throws Exception {
        mockMvc.perform(get("/api/farmer/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/farmer/me/transactions")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/buyer/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/buyer/me/purchases")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/markets/me")).andExpect(status().isUnauthorized());
    }

    private Farmer farmer(String name, String code, Market market) {
        Farmer record = new Farmer();
        record.setName(name);
        record.setFarmerCode(code);
        record.setMarket(market);
        record.setStatus("ACTIVE");
        return farmerRepository.save(record);
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
