package com.agrimandi.api.service;

import com.agrimandi.api.dto.*;
import com.agrimandi.api.entity.*;
import com.agrimandi.api.repository.*;
import com.agrimandi.api.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End Integration Test for AgriMandi AI Demo Scenario.
 * Note: Docker is unavailable in this environment, so this test uses H2 in-memory DB
 * for verifying application context, business logic, and JPA interactions.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DemoScenarioE2ETest {

    @Autowired private LotService lotService;
    @Autowired private AuctionService auctionService;
    @Autowired private PaymentService paymentService;
    @Autowired private AssetService assetService;
    @Autowired private TransactionService transactionService;

    @Autowired private MarketRepository marketRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FarmerRepository farmerRepository;
    @Autowired private BuyerRepository buyerRepository;
    @Autowired private CommodityRepository commodityRepository;
    @Autowired private AssetTypeRepository assetTypeRepository;

    private User agentUser;
    private Market market;
    private Farmer farmer;
    private Buyer buyerA, buyerB, buyerC;
    private Commodity tomato;
    private AssetType crate;

    @BeforeEach
    void setupData() {
        market = new Market();
        market.setName("Pune APMC");
        market.setCode("PUN01");
        market.setStatus("ACTIVE");
        market = marketRepository.save(market);

        agentUser = new User();
        agentUser.setUsername("agent.demo");
        agentUser.setPasswordHash("pass");
        agentUser.setRole("AGENT");
        agentUser.setMarket(market);
        agentUser.setStatus("ACTIVE");
        agentUser = userRepository.save(agentUser);

        farmer = new Farmer();
        farmer.setName("Ramesh Patil");
        farmer.setMarket(market);
        farmer.setStatus("ACTIVE");
        farmer = farmerRepository.save(farmer);

        buyerA = createBuyer("Buyer A");
        buyerB = createBuyer("Buyer B");
        buyerC = createBuyer("Buyer C");

        tomato = new Commodity();
        tomato.setName("Tomato");
        tomato.setActiveStatus(true);
        tomato = commodityRepository.save(tomato);

        crate = new AssetType();
        crate.setName("Standard Crate");
        crate.setReturnable(true);
        crate.setActive(true);
        crate.setMarket(market);
        crate = assetTypeRepository.save(crate);

        CustomUserDetails userDetails = new CustomUserDetails(agentUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    private Buyer createBuyer(String name) {
        Buyer b = new Buyer();
        b.setName(name);
        b.setMarket(market);
        b.setStatus("ACTIVE");
        return buyerRepository.save(b);
    }

    @Test
    void executeFullDemoScenario() {
        // 1. Agent creates a lot
        LotRequestDTO lotReq = new LotRequestDTO();
        lotReq.setFarmerId(farmer.getId());
        lotReq.setCommodityId(tomato.getId());
        lotReq.setAssetTypeId(crate.getId());
        Lot lot = lotService.createLot(lotReq);

        assertThat(lot.getStatus()).isEqualTo("PENDING_WEIGHMENT");
        assertThat(lot.getMarket().getId()).isEqualTo(market.getId());

        // 2. Agent records weighment
        WeighmentRequestDTO wReq = new WeighmentRequestDTO();
        wReq.setGrossWeight(new BigDecimal("520.5"));
        wReq.setTareWeight(new BigDecimal("20.5"));
        wReq.setWeightUnit("KG");
        lotService.recordWeighment(lot.getId(), wReq);
        
        // Refresh lot via repo if needed, but lotService updates it.
        // The lot quantity should be 500.0
        
        // 3. Agent starts the auction
        Auction auction = auctionService.startAuction(lot.getId());
        assertThat(auction.getStatus()).isEqualTo("IN_PROGRESS");

        // 4. Agent records three bids: A(18), B(20.5), C(19.25)
        auctionService.recordBid(lot.getId(), createBid(buyerA, "18.00"));
        BidResponseDTO bidB = auctionService.recordBid(lot.getId(), createBid(buyerB, "20.50"));
        auctionService.recordBid(lot.getId(), createBid(buyerC, "19.25"));

        // 5. Agent closes auction
        auctionService.closeAuction(lot.getId());

        // 6. Confirm winner (Buyer B)
        WinnerConfirmationDTO winReq = new WinnerConfirmationDTO();
        winReq.setWinningBidId(bidB.getId());
        TransactionResponseDTO txResp = auctionService.confirmWinner(lot.getId(), winReq);
        
        assertThat(txResp.getAmount().getGross()).isEqualByComparingTo(new BigDecimal("10250.00"));
        assertThat(txResp.getAmount().getNet()).isEqualByComparingTo(new BigDecimal("10250.00"));

        // 7. Agent records partial payment: 7000
        PaymentRequestDTO payReq = new PaymentRequestDTO();
        payReq.setAmount(new BigDecimal("7000.00"));
        payReq.setPaymentMethod("CASH");
        paymentService.recordPayment(txResp.getId(), payReq);

        BigDecimal pending = paymentService.getPendingAmount(txResp.getId());
        assertThat(pending).isEqualByComparingTo(new BigDecimal("3250.00"));

        // 8. Agent issues 40 returnable crates
        AssetIssueReturnDTO issueReq = new AssetIssueReturnDTO();
        issueReq.setAssetTypeId(crate.getId());
        issueReq.setQuantity(40);
        assetService.issueAsset(txResp.getId(), issueReq);

        // 9. Agent records 25 crates returned
        AssetIssueReturnDTO returnReq = new AssetIssueReturnDTO();
        returnReq.setAssetTypeId(crate.getId());
        returnReq.setQuantity(25);
        assetService.returnAsset(txResp.getId(), returnReq);

        // 10. Verify final transaction state for Farmer
        CustomUserDetails farmerDetails = new CustomUserDetails(createFarmerUser());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(farmerDetails, null, farmerDetails.getAuthorities())
        );

        TransactionResponseDTO finalTx = transactionService.getTransactionDetail(txResp.getId());
        assertThat(finalTx.getAmount().getPending()).isEqualByComparingTo(new BigDecimal("3250.00"));
        assertThat(finalTx.getAssets().getOutstanding()).isEqualTo(15);
    }

    private BidRequestDTO createBid(Buyer b, String amt) {
        BidRequestDTO req = new BidRequestDTO();
        req.setBuyerId(b.getId());
        req.setBidAmount(new BigDecimal(amt));
        req.setPriceUnit("KG");
        return req;
    }
    
    private User createFarmerUser() {
        User u = new User();
        u.setUsername("farmer1");
        u.setRole("FARMER");
        u.setFarmer(farmer);
        return u;
    }
}
