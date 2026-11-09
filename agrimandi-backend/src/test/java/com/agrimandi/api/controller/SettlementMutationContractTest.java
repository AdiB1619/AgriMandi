package com.agrimandi.api.controller;

import com.agrimandi.api.dto.AssetIssueReturnDTO;
import com.agrimandi.api.entity.AssetType;
import com.agrimandi.api.entity.Auction;
import com.agrimandi.api.entity.Bid;
import com.agrimandi.api.entity.Buyer;
import com.agrimandi.api.entity.Commodity;
import com.agrimandi.api.entity.Farmer;
import com.agrimandi.api.entity.Lot;
import com.agrimandi.api.entity.Market;
import com.agrimandi.api.entity.Transaction;
import com.agrimandi.api.entity.User;
import com.agrimandi.api.repository.AssetTypeRepository;
import com.agrimandi.api.repository.AuctionRepository;
import com.agrimandi.api.repository.BidRepository;
import com.agrimandi.api.repository.BuyerRepository;
import com.agrimandi.api.repository.CommodityRepository;
import com.agrimandi.api.repository.FarmerRepository;
import com.agrimandi.api.repository.LotRepository;
import com.agrimandi.api.repository.MarketRepository;
import com.agrimandi.api.repository.TransactionRepository;
import com.agrimandi.api.repository.UserRepository;
import com.agrimandi.api.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract of the settlement mutation endpoints.
 *
 * <p>These endpoints used to return the {@code Payment} / {@code AssetTransaction} entities, whose
 * lazily-loaded {@code transaction} association does not serialise into valid JSON, so the browser
 * received a truncated body and the screen silently kept its old numbers. Returning the refreshed
 * transaction DTO fixes that and gives the caller the recalculated balance, so this test pins it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SettlementMutationContractTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MarketRepository marketRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FarmerRepository farmerRepository;
    @Autowired private BuyerRepository buyerRepository;
    @Autowired private CommodityRepository commodityRepository;
    @Autowired private AssetTypeRepository assetTypeRepository;
    @Autowired private LotRepository lotRepository;
    @Autowired private AuctionRepository auctionRepository;
    @Autowired private BidRepository bidRepository;
    @Autowired private TransactionRepository transactionRepository;

    private User agent;
    private Transaction transaction;
    private AssetType crate;

    @BeforeEach
    void setUp() {
        Market market = new Market();
        market.setName("Pune APMC");
        market.setCode("PUN01");
        market.setStatus("ACTIVE");
        market = marketRepository.save(market);

        User agentUser = new User();
        agentUser.setUsername("agent.settle");
        agentUser.setPasswordHash("test-hash");
        agentUser.setRole("AGENT");
        agentUser.setStatus("ACTIVE");
        agentUser.setMarket(market);
        agent = userRepository.save(agentUser);

        Farmer farmer = new Farmer();
        farmer.setName("Ramesh Patil");
        farmer.setMarket(market);
        farmer.setStatus("ACTIVE");
        farmer = farmerRepository.save(farmer);

        Buyer buyer = new Buyer();
        buyer.setName("Suresh Traders");
        buyer.setMarket(market);
        buyer.setStatus("ACTIVE");
        buyer = buyerRepository.save(buyer);

        Commodity tomato = new Commodity();
        tomato.setName("Tomato");
        tomato.setActiveStatus(true);
        tomato = commodityRepository.save(tomato);

        crate = new AssetType();
        crate.setName("Standard Crate");
        crate.setReturnable(true);
        crate.setActive(true);
        crate.setMarket(market);
        crate = assetTypeRepository.save(crate);

        Lot lot = new Lot();
        lot.setLotNumber("LOT-2026-000001");
        lot.setMarket(market);
        lot.setFarmer(farmer);
        lot.setCommodity(tomato);
        lot.setAssetType(crate);
        lot.setQuantity(new BigDecimal("500.00"));
        lot.setQuantityUnit("KG");
        lot.setPackagingType("Jute Bag");
        lot.setStatus("TRANSACTION_CREATED");
        lot = lotRepository.save(lot);

        Auction auction = new Auction();
        auction.setLot(lot);
        auction.setStatus("COMPLETED");
        auction.setStartedAt(OffsetDateTime.now().minusHours(3));
        auction.setEndedAt(OffsetDateTime.now().minusHours(2));
        auction = auctionRepository.save(auction);

        Bid bid = new Bid();
        bid.setAuction(auction);
        bid.setBuyer(buyer);
        bid.setBidAmount(new BigDecimal("20.50"));
        bid.setPriceUnit("KG");
        bid.setBidTime(OffsetDateTime.now().minusHours(3));
        bid.setStatus("WINNING");
        bid = bidRepository.save(bid);

        auction.setWinnerBid(bid);
        auction.setWinnerBuyer(buyer);
        auctionRepository.save(auction);

        transaction = new Transaction();
        transaction.setTransactionNumber("TXN-2026-000001");
        transaction.setLot(lot);
        transaction.setAuction(auction);
        transaction.setFarmer(farmer);
        transaction.setBuyer(buyer);
        transaction.setWinningBid(bid);
        transaction.setQuantity(new BigDecimal("500.00"));
        transaction.setQuantityUnit("KG");
        transaction.setPriceUnit("KG");
        transaction.setWinningRate(new BigDecimal("20.50"));
        transaction.setGrossAmount(new BigDecimal("10250.00"));
        transaction.setNetAmount(new BigDecimal("10250.00"));
        transaction.setAdjustments(BigDecimal.ZERO);
        transaction.setStatus("CREATED");
        transaction.setTransactionDate(OffsetDateTime.now().minusHours(2));
        transaction = transactionRepository.save(transaction);
    }

    @Test
    @DisplayName("Recording a payment returns the recalculated transaction, not a broken entity graph")
    void recordPaymentReturnsRefreshedTransaction() throws Exception {
        mockMvc.perform(post("/api/transactions/" + transaction.getId() + "/payments")
                        .with(as(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":7000,\"paymentMethod\":\"CASH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionNumber").value("TXN-2026-000001"))
                .andExpect(jsonPath("$.amount.net").value(10250.00))
                .andExpect(jsonPath("$.amount.paid").value(7000.00))
                .andExpect(jsonPath("$.amount.pending").value(3250.00))
                .andExpect(jsonPath("$.paymentStatus").value("PARTIALLY_PAID"));

        mockMvc.perform(get("/api/transactions/" + transaction.getId() + "/payments/pending-amount")
                        .with(as(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingAmount").value(3250.00));

        mockMvc.perform(post("/api/transactions/" + transaction.getId() + "/payments")
                        .with(as(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":3250,\"paymentMethod\":\"UPI\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount.paid").value(10250.00))
                .andExpect(jsonPath("$.amount.pending").value(0.00))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
    }

    @Test
    @DisplayName("Issuing and returning assets return the recalculated transaction")
    void assetMovementsReturnRefreshedTransaction() throws Exception {
        String issued = """
                {"assetTypeId":"%s","quantity":40}
                """.formatted(crate.getId());

        mockMvc.perform(post("/api/transactions/" + transaction.getId() + "/assets/issue")
                        .with(as(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(issued))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assets.assetTypeName").value("Standard Crate"))
                .andExpect(jsonPath("$.assets.issued").value(40))
                .andExpect(jsonPath("$.assets.returned").value(0))
                .andExpect(jsonPath("$.assets.outstanding").value(40));

        String returned = """
                {"assetTypeId":"%s","quantity":25}
                """.formatted(crate.getId());

        mockMvc.perform(post("/api/transactions/" + transaction.getId() + "/assets/return")
                        .with(as(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(returned))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assets.issued").value(40))
                .andExpect(jsonPath("$.assets.returned").value(25))
                .andExpect(jsonPath("$.assets.outstanding").value(15));
    }

    @Test
    @DisplayName("A payment is rejected when it would exceed the outstanding balance")
    void overpaymentIsRejected() throws Exception {
        mockMvc.perform(post("/api/transactions/" + transaction.getId() + "/payments")
                        .with(as(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":99999,\"paymentMethod\":\"CASH\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Returning more assets than were issued is rejected")
    void overReturnIsRejected() throws Exception {
        String issued = """
                {"assetTypeId":"%s","quantity":5}
                """.formatted(crate.getId());
        mockMvc.perform(post("/api/transactions/" + transaction.getId() + "/assets/issue")
                        .with(as(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(issued))
                .andExpect(status().isOk());

        String tooMany = """
                {"assetTypeId":"%s","quantity":9}
                """.formatted(crate.getId());
        mockMvc.perform(post("/api/transactions/" + transaction.getId() + "/assets/return")
                        .with(as(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tooMany))
                .andExpect(status().is4xxClientError());
    }

    private RequestPostProcessor as(User user) {
        CustomUserDetails details = new CustomUserDetails(user);
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        return authentication(authentication);
    }
}
