package com.agrimandi.api.controller;

import com.agrimandi.api.dto.AssetIssueReturnDTO;
import com.agrimandi.api.dto.BidRequestDTO;
import com.agrimandi.api.dto.BidResponseDTO;
import com.agrimandi.api.dto.LotRequestDTO;
import com.agrimandi.api.dto.PaymentRequestDTO;
import com.agrimandi.api.dto.WinnerConfirmationDTO;
import com.agrimandi.api.dto.WeighmentRequestDTO;
import com.agrimandi.api.entity.AssetType;
import com.agrimandi.api.entity.Buyer;
import com.agrimandi.api.entity.Commodity;
import com.agrimandi.api.entity.Farmer;
import com.agrimandi.api.entity.Market;
import com.agrimandi.api.entity.User;
import com.agrimandi.api.repository.AssetTypeRepository;
import com.agrimandi.api.repository.BuyerRepository;
import com.agrimandi.api.repository.CommodityRepository;
import com.agrimandi.api.repository.FarmerRepository;
import com.agrimandi.api.repository.MarketRepository;
import com.agrimandi.api.repository.UserRepository;
import com.agrimandi.api.security.CustomUserDetails;
import com.agrimandi.api.service.AssetService;
import com.agrimandi.api.service.AuctionService;
import com.agrimandi.api.service.LotService;
import com.agrimandi.api.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the JSON contract the reporting UI binds to, using the Ramesh / Tomato validation
 * scenario driven through the real business services.
 *
 * <p>The expected figures (500 kg at 20.50 = 10,250; 7,000 paid; 3,250 outstanding; 40 crates
 * issued, 25 returned, 15 outstanding) are asserted here only — production code derives them
 * from persisted records.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportApiContractTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private LotService lotService;
    @Autowired private AuctionService auctionService;
    @Autowired private PaymentService paymentService;
    @Autowired private AssetService assetService;

    @Autowired private MarketRepository marketRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FarmerRepository farmerRepository;
    @Autowired private BuyerRepository buyerRepository;
    @Autowired private CommodityRepository commodityRepository;
    @Autowired private AssetTypeRepository assetTypeRepository;

    private User admin;

    @BeforeEach
    void setUp() {
        Market market = new Market();
        market.setName("Pune APMC");
        market.setCode("PUN01");
        market.setStatus("ACTIVE");
        market = marketRepository.save(market);

        admin = new User();
        admin.setUsername("secretary.contract");
        admin.setPasswordHash("test-hash");
        admin.setRole("ADMIN");
        admin.setStatus("ACTIVE");
        admin.setMarket(market);
        admin = userRepository.save(admin);

        Farmer farmer = new Farmer();
        farmer.setName("Ramesh Patil");
        farmer.setFarmerCode("FARM-001");
        farmer.setMarket(market);
        farmer.setStatus("ACTIVE");
        farmer = farmerRepository.save(farmer);

        Buyer buyerA = buyer("Buyer A", market);
        Buyer buyerB = buyer("Buyer B", market);
        Buyer buyerC = buyer("Buyer C", market);

        Commodity tomato = new Commodity();
        tomato.setName("Tomato");
        tomato.setCode("TOM");
        tomato.setDefaultPriceUnit("KG");
        tomato.setActiveStatus(true);
        tomato = commodityRepository.save(tomato);

        AssetType crate = new AssetType();
        crate.setName("Standard Crate");
        crate.setCode("CRATE");
        crate.setReturnable(true);
        crate.setActive(true);
        crate.setMarket(market);
        crate = assetTypeRepository.save(crate);

        CustomUserDetails details = new CustomUserDetails(admin);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));

        LotRequestDTO lotRequest = new LotRequestDTO();
        lotRequest.setFarmerId(farmer.getId());
        lotRequest.setCommodityId(tomato.getId());
        lotRequest.setAssetTypeId(crate.getId());
        var lot = lotService.createLot(lotRequest);

        WeighmentRequestDTO weighment = new WeighmentRequestDTO();
        weighment.setGrossWeight(new BigDecimal("520"));
        weighment.setTareWeight(new BigDecimal("20"));
        weighment.setWeightUnit("KG");
        lotService.recordWeighment(lot.getId(), weighment);

        auctionService.startAuction(lot.getId());
        auctionService.recordBid(lot.getId(), bid(buyerA, "18.00"));
        BidResponseDTO winning = auctionService.recordBid(lot.getId(), bid(buyerB, "20.50"));
        auctionService.recordBid(lot.getId(), bid(buyerC, "19.25"));
        auctionService.closeAuction(lot.getId());

        WinnerConfirmationDTO winner = new WinnerConfirmationDTO();
        winner.setWinningBidId(winning.getId());
        var transaction = auctionService.confirmWinner(lot.getId(), winner);

        PaymentRequestDTO payment = new PaymentRequestDTO();
        payment.setAmount(new BigDecimal("7000"));
        payment.setPaymentMethod("CASH");
        paymentService.recordPayment(transaction.getId(), payment);

        AssetIssueReturnDTO issue = new AssetIssueReturnDTO();
        issue.setAssetTypeId(crate.getId());
        issue.setQuantity(40);
        assetService.issueAsset(transaction.getId(), issue);

        AssetIssueReturnDTO returned = new AssetIssueReturnDTO();
        returned.setAssetTypeId(crate.getId());
        returned.setQuantity(25);
        assetService.returnAsset(transaction.getId(), returned);
    }

    @Test
    @DisplayName("Oversight endpoint returns today's and outstanding figures as numeric JSON")
    void overview_contract() throws Exception {
        mockMvc.perform(get("/api/reports/overview").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marketId").isNotEmpty())
                .andExpect(jsonPath("$.generatedAt").isNotEmpty())
                .andExpect(jsonPath("$.todayLots.total").value(1))
                .andExpect(jsonPath("$.todayTransactions.total").value(1))
                .andExpect(jsonPath("$.todayAuctionValue.total").value(10250.0))
                .andExpect(jsonPath("$.todayPaymentCollection.total").value(7000.0))
                .andExpect(jsonPath("$.totalOutstandingPayments.total").value(3250.0))
                .andExpect(jsonPath("$.outstandingAssets.totalIssued").value(40))
                .andExpect(jsonPath("$.outstandingAssets.totalReturned").value(25))
                .andExpect(jsonPath("$.outstandingAssets.totalOutstanding").value(15))
                .andExpect(jsonPath("$.activeAuctions.total").value(0))
                .andExpect(jsonPath("$.recentTransactions", hasSize(1)))
                .andExpect(jsonPath("$.recentAuditActivity").isArray())
                .andExpect(jsonPath("$.topBuyersByValue", hasSize(1)))
                .andExpect(jsonPath("$.topBuyersByValue[0].buyerName").value("Buyer B"))
                .andExpect(jsonPath("$.farmerActivity", hasSize(1)))
                .andExpect(jsonPath("$.farmerActivity[0].farmerName").value("Ramesh Patil"))
                .andExpect(jsonPath("$.exceptions").isArray());
    }

    @Test
    @DisplayName("Transaction summary contract")
    void transactionSummary_contract() throws Exception {
        mockMvc.perform(get("/api/reports/transactions").param("period", "CURRENT_MONTH").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.range.from").isNotEmpty())
                .andExpect(jsonPath("$.range.to").isNotEmpty())
                .andExpect(jsonPath("$.aggregate.totalTransactions").value(1))
                .andExpect(jsonPath("$.aggregate.grossSaleValue").value(10250.0))
                .andExpect(jsonPath("$.aggregate.netSaleValue").value(10250.0))
                .andExpect(jsonPath("$.aggregate.totalQuantity").value(500.0))
                .andExpect(jsonPath("$.aggregate.averageSaleRate").value(20.5))
                .andExpect(jsonPath("$.totalOutstandingPayment").value(3250.0))
                .andExpect(jsonPath("$.statusBreakdown[0].status").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.statusBreakdown[0].count").value(1));
    }

    @Test
    @DisplayName("Payment and asset summary contract")
    void paymentAndAssetSummary_contract() throws Exception {
        mockMvc.perform(get("/api/reports/payments").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aggregate.totalBilled").value(10250.0))
                .andExpect(jsonPath("$.aggregate.totalReceived").value(7000.0))
                .andExpect(jsonPath("$.aggregate.totalOutstanding").value(3250.0))
                .andExpect(jsonPath("$.aggregate.partiallyPaidCount").value(1))
                .andExpect(jsonPath("$.aggregate.settledCount").value(0));

        mockMvc.perform(get("/api/reports/assets").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aggregate.totalIssued").value(40))
                .andExpect(jsonPath("$.aggregate.totalReturned").value(25))
                .andExpect(jsonPath("$.aggregate.totalOutstanding").value(15));
    }

    @Test
    @DisplayName("Auction, buyer and farmer summary contract")
    void activitySummary_contract() throws Exception {
        mockMvc.perform(get("/api/reports/auctions").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aggregate.totalLots").value(1))
                .andExpect(jsonPath("$.aggregate.totalAuctions").value(1))
                .andExpect(jsonPath("$.aggregate.completedAuctions").value(1))
                .andExpect(jsonPath("$.aggregate.transactionsGenerated").value(1))
                .andExpect(jsonPath("$.aggregate.totalQuantityAuctioned").value(500.0));

        mockMvc.perform(get("/api/reports/buyers").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buyers", hasSize(1)))
                .andExpect(jsonPath("$.buyers[0].purchases").value(1))
                .andExpect(jsonPath("$.buyers[0].totalPurchaseValue").value(10250.0))
                .andExpect(jsonPath("$.buyers[0].totalQuantity").value(500.0))
                .andExpect(jsonPath("$.buyers[0].outstandingPayment").value(3250.0))
                .andExpect(jsonPath("$.buyers[0].outstandingAssetRecords").value(1));

        mockMvc.perform(get("/api/reports/farmers").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.farmers", hasSize(1)))
                .andExpect(jsonPath("$.farmers[0].lots").value(1))
                .andExpect(jsonPath("$.farmers[0].totalQuantity").value(500.0))
                .andExpect(jsonPath("$.farmers[0].transactions").value(1))
                .andExpect(jsonPath("$.farmers[0].totalTransactionValue").value(10250.0));
    }

    @Test
    @DisplayName("Payment ledger contract matches the ledger screen columns")
    void paymentLedger_contract() throws Exception {
        mockMvc.perform(get("/api/reports/payment-ledger").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].transactionNumber").isNotEmpty())
                .andExpect(jsonPath("$.content[0].transactionDate").isNotEmpty())
                .andExpect(jsonPath("$.content[0].lotNumber").isNotEmpty())
                .andExpect(jsonPath("$.content[0].commodity").value("Tomato"))
                .andExpect(jsonPath("$.content[0].farmerName").value("Ramesh Patil"))
                .andExpect(jsonPath("$.content[0].buyerName").value("Buyer B"))
                .andExpect(jsonPath("$.content[0].totalAmount").value(10250.0))
                .andExpect(jsonPath("$.content[0].paidAmount").value(7000.0))
                .andExpect(jsonPath("$.content[0].outstandingAmount").value(3250.0))
                .andExpect(jsonPath("$.content[0].paymentStatus").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.content[0].latestPaymentDate").isNotEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    @DisplayName("Asset ledger contract matches the asset ledger screen columns")
    void assetLedger_contract() throws Exception {
        mockMvc.perform(get("/api/reports/asset-ledger").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].transactionNumber").isNotEmpty())
                .andExpect(jsonPath("$.content[0].lotNumber").isNotEmpty())
                .andExpect(jsonPath("$.content[0].assetTypeName").value("Standard Crate"))
                .andExpect(jsonPath("$.content[0].buyerName").value("Buyer B"))
                .andExpect(jsonPath("$.content[0].quantityIssued").value(40))
                .andExpect(jsonPath("$.content[0].quantityReturned").value(25))
                .andExpect(jsonPath("$.content[0].quantityOutstanding").value(15))
                .andExpect(jsonPath("$.content[0].issueDate").isNotEmpty())
                .andExpect(jsonPath("$.content[0].latestReturnDate").isNotEmpty())
                .andExpect(jsonPath("$.content[0].assetStatus").value("PARTIALLY_RETURNED"));
    }

    @Test
    @DisplayName("Audit endpoint returns newest-first paginated rows for the admin")
    void auditContract() throws Exception {
        mockMvc.perform(get("/api/audit-logs").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.content[0].timestamp").isNotEmpty())
                .andExpect(jsonPath("$.content[0].action").isNotEmpty())
                .andExpect(jsonPath("$.content[0].entityType").isNotEmpty())
                .andExpect(jsonPath("$.content[0].entityId").isNotEmpty())
                .andExpect(jsonPath("$.content[0].actorUsername").isNotEmpty())
                .andExpect(jsonPath("$.content[0].actorRole").isNotEmpty())
                .andExpect(jsonPath("$.content[0].marketId").isNotEmpty());

        mockMvc.perform(get("/api/audit-logs/actions").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Date filtering changes the backend result for the same user")
    void dateFiltering_contract() throws Exception {
        mockMvc.perform(get("/api/reports/transactions").param("period", "YESTERDAY").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aggregate.totalTransactions").value(0));

        mockMvc.perform(get("/api/reports/transactions").param("period", "CURRENT_MONTH").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aggregate.totalTransactions").value(1));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor asAdmin() {
        CustomUserDetails details = new CustomUserDetails(admin);
        return authentication(new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    private BidRequestDTO bid(Buyer buyer, String amount) {
        BidRequestDTO request = new BidRequestDTO();
        request.setBuyerId(buyer.getId());
        request.setBidAmount(new BigDecimal(amount));
        request.setPriceUnit("KG");
        return request;
    }

    private Buyer buyer(String name, Market market) {
        Buyer buyer = new Buyer();
        buyer.setName(name);
        buyer.setBuyerCode("B-" + UUID.randomUUID().toString().substring(0, 6));
        buyer.setMarket(market);
        buyer.setStatus("ACTIVE");
        return buyerRepository.save(buyer);
    }
}
