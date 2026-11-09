package com.agrimandi.api.service;

import com.agrimandi.api.dto.AssetIssueReturnDTO;
import com.agrimandi.api.dto.BidRequestDTO;
import com.agrimandi.api.dto.BidResponseDTO;
import com.agrimandi.api.dto.LotRequestDTO;
import com.agrimandi.api.dto.PaymentRequestDTO;
import com.agrimandi.api.dto.TransactionResponseDTO;
import com.agrimandi.api.dto.WinnerConfirmationDTO;
import com.agrimandi.api.dto.WeighmentRequestDTO;
import com.agrimandi.api.dto.report.AuditLogDTO;
import com.agrimandi.api.dto.report.PageResponse;
import com.agrimandi.api.dto.report.ReportDTOs.AdminOversightResponse;
import com.agrimandi.api.dto.report.ReportDTOs.AssetLedgerRow;
import com.agrimandi.api.dto.report.ReportDTOs.AssetSummaryResponse;
import com.agrimandi.api.dto.report.ReportDTOs.AuctionSummaryResponse;
import com.agrimandi.api.dto.report.ReportDTOs.BuyerActivity;
import com.agrimandi.api.dto.report.ReportDTOs.BuyerSummaryResponse;
import com.agrimandi.api.dto.report.ReportDTOs.DateRange;
import com.agrimandi.api.dto.report.ReportDTOs.FarmerActivity;
import com.agrimandi.api.dto.report.ReportDTOs.FarmerSummaryResponse;
import com.agrimandi.api.dto.report.ReportDTOs.PaymentLedgerRow;
import com.agrimandi.api.dto.report.ReportDTOs.PaymentSummaryResponse;
import com.agrimandi.api.dto.report.ReportDTOs.ReportPeriod;
import com.agrimandi.api.dto.report.ReportDTOs.TransactionAggregate;
import com.agrimandi.api.dto.report.ReportDTOs.TransactionSummaryResponse;
import com.agrimandi.api.entity.AssetType;
import com.agrimandi.api.entity.Auction;
import com.agrimandi.api.entity.Buyer;
import com.agrimandi.api.entity.Commodity;
import com.agrimandi.api.entity.Farmer;
import com.agrimandi.api.entity.Lot;
import com.agrimandi.api.entity.Market;
import com.agrimandi.api.entity.User;
import com.agrimandi.api.repository.AssetTypeRepository;
import com.agrimandi.api.repository.BuyerRepository;
import com.agrimandi.api.repository.CommodityRepository;
import com.agrimandi.api.repository.FarmerRepository;
import com.agrimandi.api.repository.MarketRepository;
import com.agrimandi.api.repository.UserRepository;
import com.agrimandi.api.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reporting, ledger and audit-trail behaviour on the H2 test database.
 *
 * <p>Every figure asserted here is derived from records created through the real services, so
 * the numbers mirror the Ramesh / Tomato validation scenario (500 kg at 20.50 = 10,250 with
 * 7,000 paid, 3,250 outstanding and 40 crates issued / 25 returned / 15 outstanding) without
 * any of that data being hard-coded in production code.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportServiceTest {

    @Autowired private ReportService reportService;
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

    private Market marketA;
    private Market marketB;
    private Market emptyMarket;

    private User agentA;
    private User agentB;
    private User adminA;
    private User farmerUserA;
    private User buyerUserB;

    private Farmer farmerA;
    private Buyer buyerA1;
    private Buyer buyerA2;
    private Buyer buyerA3;
    private Buyer buyerB1;
    private Commodity tomato;
    private AssetType crateA;

    private DateRange currentMonth;
    private DateRange yesterday;

    @BeforeEach
    void setUp() {
        marketA = market("Pune APMC", "PUN01");
        marketB = market("Nashik APMC", "NAS02");
        emptyMarket = market("Solapur APMC", "SOL03");

        tomato = new Commodity();
        tomato.setName("Tomato");
        tomato.setCode("TOM");
        tomato.setDefaultPriceUnit("KG");
        tomato.setActiveStatus(true);
        tomato = commodityRepository.save(tomato);

        agentA = user("agent.pune", "AGENT", marketA, null, null);
        agentB = user("agent.nashik", "AGENT", marketB, null, null);
        adminA = user("secretary.pune", "ADMIN", marketA, null, null);

        farmerA = farmer("Ramesh Patil", marketA);
        farmerUserA = user("farmer.ramesh", "FARMER", marketA, farmerA, null);

        buyerA1 = buyer("Buyer A", marketA);
        buyerA2 = buyer("Buyer B", marketA);
        buyerA3 = buyer("Buyer C", marketA);
        buyerB1 = buyer("Nashik Buyer", marketB);
        buyerUserB = user("buyer.b", "BUYER", marketA, null, buyerA2);

        crateA = new AssetType();
        crateA.setName("Standard Crate");
        crateA.setCode("CRATE");
        crateA.setReturnable(true);
        crateA.setActive(true);
        crateA.setMarket(marketA);
        crateA = assetTypeRepository.save(crateA);

        currentMonth = reportService.resolveRange(ReportPeriod.CURRENT_MONTH, null, null);
        yesterday = reportService.resolveRange(ReportPeriod.YESTERDAY, null, null);

        // Full Ramesh / Tomato scenario in market A.
        runFullScenario(agentA, farmerA, marketA, tomato, crateA, buyerA1, buyerA2, buyerA3,
                new BigDecimal("520"), new BigDecimal("20"), new BigDecimal("18.00"),
                new BigDecimal("20.50"), new BigDecimal("19.25"), buyerA2,
                new BigDecimal("7000"), 40, 25);

        // Independent transaction in market B, to prove isolation.
        runFullScenario(agentB, farmer("Suresh Jadhav", marketB), marketB, tomato, null, buyerB1,
                buyerB1, buyerB1, new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("15.00"),
                new BigDecimal("15.00"), new BigDecimal("12.00"), buyerB1, BigDecimal.ZERO, 0, 0);
    }

    // ── 1. Report totals ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Transaction summary totals are derived from persisted records")
    void transactionSummary_reportsPersistedTotals() {
        authenticate(agentA);
        TransactionSummaryResponse response = reportService.transactionSummary(currentMonth);

        assertThat(response.aggregate().totalTransactions()).isEqualTo(1L);
        assertThat(response.aggregate().grossSaleValue()).isEqualByComparingTo("10250.00");
        assertThat(response.aggregate().netSaleValue()).isEqualByComparingTo("10250.00");
        assertThat(response.aggregate().totalQuantity()).isEqualByComparingTo("500");
        assertThat(response.aggregate().averageSaleRate()).isEqualByComparingTo("20.50");
        assertThat(response.totalOutstandingPayment()).isEqualByComparingTo("3250.00");
        assertThat(response.statusBreakdown())
                .singleElement()
                .satisfies(status -> {
                    assertThat(status.status()).isEqualTo("PARTIALLY_PAID");
                    assertThat(status.count()).isEqualTo(1L);
                });
    }

    @Test
    @DisplayName("Payment summary reports billed, received and outstanding")
    void paymentSummary_reportsBilledReceivedOutstanding() {
        authenticate(agentA);
        PaymentSummaryResponse response = reportService.paymentSummary(currentMonth);

        assertThat(response.aggregate().totalBilled()).isEqualByComparingTo("10250.00");
        assertThat(response.aggregate().totalReceived()).isEqualByComparingTo("7000.00");
        assertThat(response.aggregate().totalOutstanding()).isEqualByComparingTo("3250.00");
        assertThat(response.aggregate().partiallyPaidCount()).isEqualTo(1L);
        assertThat(response.aggregate().settledCount()).isZero();
    }

    @Test
    @DisplayName("Asset summary reports issued, returned and outstanding quantities")
    void assetSummary_reportsMovementTotals() {
        authenticate(agentA);
        AssetSummaryResponse response = reportService.assetSummary(currentMonth);

        assertThat(response.aggregate().totalIssued()).isEqualTo(40L);
        assertThat(response.aggregate().totalReturned()).isEqualTo(25L);
        assertThat(response.aggregate().totalOutstanding()).isEqualTo(15L);
    }

    @Test
    @DisplayName("Auction summary reports lot, auction, tie and transaction counts")
    void auctionSummary_reportsAuctionActivity() {
        authenticate(agentA);
        AuctionSummaryResponse response = reportService.auctionSummary(currentMonth);

        assertThat(response.aggregate().totalLots()).isEqualTo(1L);
        assertThat(response.aggregate().totalAuctions()).isEqualTo(1L);
        assertThat(response.aggregate().completedAuctions()).isEqualTo(1L);
        assertThat(response.aggregate().transactionsGenerated()).isEqualTo(1L);
        assertThat(response.aggregate().totalQuantityAuctioned()).isEqualByComparingTo("500");
        assertThat(response.aggregate().tieCases()).isZero();
    }

    @Test
    @DisplayName("Tie cases are counted from the persisted tie flag after winner confirmation")
    void auctionSummary_countsTieCases() {
        authenticate(agentA);
        Farmer farmer = farmer("Tie Farmer", marketA);
        runFullScenario(agentA, farmer, marketA, tomato, null, buyerA1, buyerA2, buyerA3,
                new BigDecimal("80"), BigDecimal.ZERO, new BigDecimal("10.00"),
                new BigDecimal("10.00"), new BigDecimal("10.00"), buyerA2, BigDecimal.ZERO, 0, 0);

        AuctionSummaryResponse response = reportService.auctionSummary(currentMonth);
        assertThat(response.aggregate().tieCases()).isEqualTo(1L);
        assertThat(response.aggregate().completedAuctions()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Buyer summary reports purchase value, outstanding payment and outstanding assets")
    void buyerSummary_reportsBuyerActivity() {
        authenticate(agentA);
        BuyerSummaryResponse response = reportService.buyerSummary(currentMonth, null);

        assertThat(response.buyers()).hasSize(1);
        BuyerActivity row = response.buyers().get(0);
        assertThat(row.buyerId()).isEqualTo(buyerA2.getId());
        assertThat(row.purchases()).isEqualTo(1L);
        assertThat(row.totalPurchaseValue()).isEqualByComparingTo("10250.00");
        assertThat(row.totalQuantity()).isEqualByComparingTo("500");
        assertThat(row.outstandingPayment()).isEqualByComparingTo("3250.00");
        assertThat(row.outstandingAssetRecords()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Farmer summary reports lots, quantity, value and outstanding payment")
    void farmerSummary_reportsFarmerActivity() {
        authenticate(agentA);
        FarmerSummaryResponse response = reportService.farmerSummary(currentMonth, null);

        assertThat(response.farmers()).hasSize(1);
        FarmerActivity row = response.farmers().get(0);
        assertThat(row.farmerId()).isEqualTo(farmerA.getId());
        assertThat(row.lots()).isEqualTo(1L);
        assertThat(row.totalQuantity()).isEqualByComparingTo("500");
        assertThat(row.transactions()).isEqualTo(1L);
        assertThat(row.totalTransactionValue()).isEqualByComparingTo("10250.00");
        assertThat(row.outstandingPayment()).isEqualByComparingTo("3250.00");
    }

    // ── 2. Date filtering ────────────────────────────────────────────────────

    @Test
    @DisplayName("Date filtering is applied server side")
    void dateFiltering_excludesTransactionsOutsideThePeriod() {
        authenticate(agentA);

        assertThat(reportService.transactionSummary(yesterday).aggregate().totalTransactions()).isZero();
        assertThat(reportService.paymentSummary(yesterday).aggregate().totalBilled()).isEqualByComparingTo("0");
        assertThat(reportService.assetSummary(yesterday).aggregate().totalIssued()).isZero();
        assertThat(reportService.auctionSummary(yesterday).aggregate().totalAuctions()).isZero();
        assertThat(reportService.buyerSummary(yesterday, null).buyers()).isEmpty();
        assertThat(reportService.farmerSummary(yesterday, null).farmers()).isEmpty();
    }

    @Test
    @DisplayName("A custom range that excludes today returns no data")
    void dateFiltering_customRangeRespected() {
        authenticate(agentA);
        LocalDate lastYear = LocalDate.now().minusYears(1);
        DateRange range = reportService.resolveRange(ReportPeriod.CUSTOM,
                lastYear.withDayOfYear(1), lastYear.withDayOfYear(31));

        assertThat(reportService.transactionSummary(range).aggregate().totalTransactions()).isZero();
    }

    @Test
    @DisplayName("A custom range without both dates is rejected")
    void dateFiltering_customRangeRequiresBothDates() {
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                com.agrimandi.api.exception.BusinessValidationException.class,
                () -> reportService.resolveRange(ReportPeriod.CUSTOM, LocalDate.now(), null)))
                .hasMessageContaining("from");
    }

    @Test
    @DisplayName("Today and current month resolve to server side windows")
    void dateFiltering_periodsResolveServerSide() {
        DateRange today = reportService.resolveRange(ReportPeriod.TODAY, null, null);
        assertThat(today.to()).isAfter(today.from());
        assertThat(java.time.temporal.ChronoUnit.DAYS.between(today.from().toLocalDate(), today.to().toLocalDate()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Explicit from/to dates are honoured even without period=CUSTOM")
    void dateFiltering_explicitDatesWithoutPeriodAreNotIgnored() {
        LocalDate lastYear = LocalDate.now().minusYears(1);
        DateRange range = reportService.resolveRange(null,
                lastYear.withDayOfYear(1), lastYear.withDayOfYear(31));

        authenticate(agentA);
        assertThat(reportService.transactionSummary(range).aggregate().totalTransactions()).isZero();
    }

    @Test
    @DisplayName("Omitting the period entirely still defaults to the current month")
    void dateFiltering_noArgumentsDefaultsToCurrentMonth() {
        DateRange range = reportService.resolveRange(null, null, null);

        assertThat(range).isEqualTo(reportService.resolveRange(ReportPeriod.CURRENT_MONTH, null, null));
    }

    @Test
    @DisplayName("An average sale rate is withheld when the period mixes price units")
    void transactionSummary_averageSaleRateWithheldAcrossMixedPriceUnits() {
        authenticate(agentA);
        assertThat(reportService.transactionSummary(currentMonth).aggregate().averageSaleRate())
                .isEqualByComparingTo("20.50");

        addCratePricedTransactionInMarketA();

        TransactionAggregate aggregate = reportService.transactionSummary(currentMonth).aggregate();
        assertThat(aggregate.totalTransactions()).isEqualTo(2L);
        assertThat(aggregate.averageSaleRate()).isNull();
    }

    /**
     * Adds a second transaction to market A that is priced per CRATE rather than per KG, so the
     * period under report contains two different price units.
     */
    private void addCratePricedTransactionInMarketA() {
        Commodity cratePriced = new Commodity();
        cratePriced.setName("Onion");
        cratePriced.setCode("ONI");
        cratePriced.setDefaultPriceUnit("CRATE");
        cratePriced.setActiveStatus(true);
        cratePriced = commodityRepository.save(cratePriced);

        authenticate(agentA);

        LotRequestDTO lotRequest = new LotRequestDTO();
        lotRequest.setFarmerId(farmerA.getId());
        lotRequest.setCommodityId(cratePriced.getId());
        Lot lot = lotService.createLot(lotRequest);

        WeighmentRequestDTO weighment = new WeighmentRequestDTO();
        weighment.setGrossWeight(new BigDecimal("60"));
        weighment.setTareWeight(new BigDecimal("10"));
        weighment.setWeightUnit("CRATE");
        lotService.recordWeighment(lot.getId(), weighment);

        auctionService.startAuction(lot.getId());
        BidRequestDTO crateBid = new BidRequestDTO();
        crateBid.setBuyerId(buyerA1.getId());
        crateBid.setBidAmount(new BigDecimal("800.00"));
        crateBid.setPriceUnit("CRATE");
        BidResponseDTO winner = auctionService.recordBid(lot.getId(), crateBid);
        auctionService.closeAuction(lot.getId());

        WinnerConfirmationDTO confirmation = new WinnerConfirmationDTO();
        confirmation.setWinningBidId(winner.getId());
        auctionService.confirmWinner(lot.getId(), confirmation);
    }

    // ── 3. Market isolation ──────────────────────────────────────────────────

    @Test
    @DisplayName("An agent never sees another market's transactions")
    void marketIsolation_agentCannotSeeOtherMarket() {
        authenticate(agentA);
        assertThat(reportService.transactionSummary(currentMonth).aggregate().totalTransactions()).isEqualTo(1L);

        authenticate(agentB);
        assertThat(reportService.transactionSummary(currentMonth).aggregate().totalTransactions()).isEqualTo(1L);
        // Market B's own transaction is 90 kg at 15.00, never market A's 10,250.
        assertThat(reportService.transactionSummary(currentMonth).aggregate().grossSaleValue())
                .isEqualByComparingTo("1350.00");
    }

    @Test
    @DisplayName("Ledgers are scoped to the caller's own market")
    void marketIsolation_ledgersAreScoped() {
        authenticate(agentA);
        PageResponse<PaymentLedgerRow> a = reportService.paymentLedger(currentMonth, null, null, null, null, null, 0, 20);
        assertThat(a.content()).hasSize(1);
        assertThat(a.content().get(0).buyerId()).isEqualTo(buyerA2.getId());

        authenticate(agentB);
        PageResponse<PaymentLedgerRow> b = reportService.paymentLedger(currentMonth, null, null, null, null, null, 0, 20);
        assertThat(b.content()).hasSize(1);
        assertThat(b.content().get(0).buyerId()).isEqualTo(buyerB1.getId());
    }

    @Test
    @DisplayName("The admin dashboard only aggregates the admin's own market")
    void marketIsolation_adminOversightIsScoped() {
        authenticate(agentB);
        AdminOversightResponse asAgentB = reportService.adminOversight();
        assertThat(asAgentB.marketId()).isEqualTo(marketB.getId().toString());
        assertThat(asAgentB.topBuyersByValue()).allSatisfy(b -> assertThat(b.buyerId()).isEqualTo(buyerB1.getId()));
        assertThat(asAgentB.recentTransactions()).allSatisfy(
                row -> assertThat(row.totalAmount()).isEqualByComparingTo("1350.00"));
    }

    // ── 4/5/6/7. Role authorization ──────────────────────────────────────────

    @Test
    @DisplayName("An admin may read the market-wide reports for their market")
    void authorization_adminCanReadReports() {
        authenticate(adminA);
        assertThat(reportService.transactionSummary(currentMonth).aggregate().totalTransactions()).isEqualTo(1L);
        assertThat(reportService.paymentSummary(currentMonth).aggregate().totalBilled()).isEqualByComparingTo("10250.00");
        assertThat(reportService.adminOversight().marketId()).isEqualTo(marketA.getId().toString());
    }

    @Test
    @DisplayName("A farmer cannot read market-wide reporting")
    void authorization_farmerIsDenied() {
        authenticate(farmerUserA);
        assertThat(denied(() -> reportService.transactionSummary(currentMonth))).isTrue();
        assertThat(denied(() -> reportService.paymentSummary(currentMonth))).isTrue();
        assertThat(denied(() -> reportService.assetSummary(currentMonth))).isTrue();
        assertThat(denied(() -> reportService.auctionSummary(currentMonth))).isTrue();
        assertThat(denied(() -> reportService.buyerSummary(currentMonth, null))).isTrue();
        assertThat(denied(() -> reportService.farmerSummary(currentMonth, null))).isTrue();
        assertThat(denied(() -> reportService.adminOversight())).isTrue();
        assertThat(denied(() -> reportService.auditTrail(currentMonth, null, null, null, null, 0, 20))).isTrue();
    }

    @Test
    @DisplayName("A buyer cannot read market-wide reporting")
    void authorization_buyerIsDenied() {
        authenticate(buyerUserB);
        assertThat(denied(() -> reportService.transactionSummary(currentMonth))).isTrue();
        assertThat(denied(() -> reportService.paymentSummary(currentMonth))).isTrue();
        assertThat(denied(() -> reportService.buyerSummary(currentMonth, null))).isTrue();
        assertThat(denied(() -> reportService.adminOversight())).isTrue();
        assertThat(denied(() -> reportService.auditTrail(currentMonth, null, null, null, null, 0, 20))).isTrue();
    }

    // ── 9. Payment ledger correctness ────────────────────────────────────────

    @Test
    @DisplayName("Payment ledger rows are correct and link to the transaction detail")
    void paymentLedger_rowCorrectness() {
        authenticate(agentA);
        PageResponse<PaymentLedgerRow> page =
                reportService.paymentLedger(currentMonth, null, null, null, null, null, 0, 20);

        assertThat(page.content()).hasSize(1);
        PaymentLedgerRow row = page.content().get(0);
        assertThat(row.transactionNumber()).isNotBlank();
        assertThat(row.lotNumber()).isNotBlank();
        assertThat(row.commodity()).isEqualTo("Tomato");
        assertThat(row.farmerName()).isEqualTo("Ramesh Patil");
        assertThat(row.buyerName()).isEqualTo("Buyer B");
        assertThat(row.totalAmount()).isEqualByComparingTo("10250.00");
        assertThat(row.paidAmount()).isEqualByComparingTo("7000.00");
        assertThat(row.outstandingAmount()).isEqualByComparingTo("3250.00");
        assertThat(row.paymentStatus()).isEqualTo("PARTIALLY_PAID");
        assertThat(row.latestPaymentDate()).isNotNull();
        assertThat(row.transactionDate()).isNotNull();
    }

    @Test
    @DisplayName("Payment ledger supports status, buyer, farmer and search filters")
    void paymentLedger_filters() {
        authenticate(agentA);

        assertThat(reportService.paymentLedger(currentMonth, "PARTIALLY_PAID", null, null, null, null, 0, 20)
                .content()).hasSize(1);
        assertThat(reportService.paymentLedger(currentMonth, "PAID", null, null, null, null, 0, 20)
                .content()).isEmpty();
        assertThat(reportService.paymentLedger(currentMonth, null, buyerA2.getId(), null, null, null, 0, 20)
                .content()).hasSize(1);
        assertThat(reportService.paymentLedger(currentMonth, null, buyerA1.getId(), null, null, null, 0, 20)
                .content()).isEmpty();
        assertThat(reportService.paymentLedger(currentMonth, null, null, farmerA.getId(), null, null, 0, 20)
                .content()).hasSize(1);
        assertThat(reportService.paymentLedger(currentMonth, null, null, null, null, "ramesh", 0, 20)
                .content()).hasSize(1);
        assertThat(reportService.paymentLedger(currentMonth, null, null, null, null, "no-such-party", 0, 20)
                .content()).isEmpty();
    }

    // ── 10. Asset ledger correctness ─────────────────────────────────────────

    @Test
    @DisplayName("Asset ledger derives issued, returned and outstanding from movements")
    void assetLedger_rowCorrectness() {
        authenticate(agentA);
        PageResponse<AssetLedgerRow> page = reportService.assetLedger(currentMonth, null, null, 0, 20);

        assertThat(page.content()).hasSize(1);
        AssetLedgerRow row = page.content().get(0);
        assertThat(row.transactionNumber()).isNotBlank();
        assertThat(row.lotNumber()).isNotBlank();
        assertThat(row.assetTypeName()).isEqualTo("Standard Crate");
        assertThat(row.buyerName()).isEqualTo("Buyer B");
        assertThat(row.quantityIssued()).isEqualTo(40);
        assertThat(row.quantityReturned()).isEqualTo(25);
        assertThat(row.quantityOutstanding()).isEqualTo(15);
        assertThat(row.issueDate()).isNotNull();
        assertThat(row.latestReturnDate()).isNotNull();
        assertThat(row.assetStatus()).isEqualTo("PARTIALLY_RETURNED");
    }

    @Test
    @DisplayName("Asset ledger status is derived from actual movement state")
    void assetLedger_statusIsDerived() {
        authenticate(agentA);
        Farmer farmer = farmer("Asset Farmer", marketA);
        runFullScenario(agentA, farmer, marketA, tomato, crateA, buyerA1, buyerA2, buyerA3,
                new BigDecimal("60"), BigDecimal.ZERO, new BigDecimal("20.00"),
                new BigDecimal("20.00"), new BigDecimal("20.00"), buyerA2, BigDecimal.ZERO, 10, 0);

        PageResponse<AssetLedgerRow> page = reportService.assetLedger(currentMonth, null, null, 0, 20);
        assertThat(page.content()).hasSize(2);
        assertThat(page.content()).extracting(AssetLedgerRow::assetStatus)
                .containsExactlyInAnyOrder("PARTIALLY_RETURNED", "OUTSTANDING");
    }

    @Test
    @DisplayName("Asset ledger supports buyer and search filters")
    void assetLedger_filters() {
        authenticate(agentA);
        assertThat(reportService.assetLedger(currentMonth, buyerA2.getId(), null, 0, 20).content()).hasSize(1);
        assertThat(reportService.assetLedger(currentMonth, buyerA1.getId(), null, 0, 20).content()).isEmpty();
        assertThat(reportService.assetLedger(currentMonth, null, "standard crate", 0, 20).content()).hasSize(1);
        assertThat(reportService.assetLedger(currentMonth, null, "unknown-asset", 0, 20).content()).isEmpty();
    }

    // ── 11. Pagination ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Payment ledger paginates server side")
    void pagination_isServerSide() {
        authenticate(agentA);
        Farmer second = farmer("Second Farmer", marketA);
        runFullScenario(agentA, second, marketA, tomato, null, buyerA1, buyerA3, buyerA1,
                new BigDecimal("70"), BigDecimal.ZERO, new BigDecimal("11.00"),
                new BigDecimal("12.00"), new BigDecimal("11.00"), buyerA3, BigDecimal.ZERO, 0, 0);

        PageResponse<PaymentLedgerRow> pageOne = reportService.paymentLedger(currentMonth, null, null, null, null, null, 0, 1);
        assertThat(pageOne.content()).hasSize(1);
        assertThat(pageOne.totalElements()).isEqualTo(2L);
        assertThat(pageOne.totalPages()).isEqualTo(2);
        assertThat(pageOne.first()).isTrue();
        assertThat(pageOne.last()).isFalse();

        PageResponse<PaymentLedgerRow> pageTwo = reportService.paymentLedger(currentMonth, null, null, null, null, null, 1, 1);
        assertThat(pageTwo.content()).hasSize(1);
        assertThat(pageTwo.last()).isTrue();
        assertThat(pageTwo.first()).isFalse();
        assertThat(pageTwo.content().get(0).transactionId()).isNotEqualTo(pageOne.content().get(0).transactionId());
    }

    // ── 12. Empty dataset behaviour ──────────────────────────────────────────

    @Test
    @DisplayName("A market without activity reports zeroes and empty lists, never placeholders")
    void emptyDataset_reportsZeroesAndEmptyLists() {
        authenticate(user("agent.empty", "AGENT", emptyMarket, null, null));

        TransactionSummaryResponse transactions = reportService.transactionSummary(currentMonth);
        assertThat(transactions.aggregate().totalTransactions()).isZero();
        assertThat(transactions.aggregate().grossSaleValue()).isEqualByComparingTo("0");
        assertThat(transactions.totalOutstandingPayment()).isEqualByComparingTo("0");
        assertThat(transactions.statusBreakdown()).isEmpty();

        assertThat(reportService.paymentSummary(currentMonth).aggregate().totalBilled()).isEqualByComparingTo("0");
        assertThat(reportService.assetSummary(currentMonth).aggregate().totalIssued()).isZero();
        assertThat(reportService.auctionSummary(currentMonth).aggregate().totalAuctions()).isZero();
        assertThat(reportService.buyerSummary(currentMonth, null).buyers()).isEmpty();
        assertThat(reportService.farmerSummary(currentMonth, null).farmers()).isEmpty();

        PageResponse<PaymentLedgerRow> ledger =
                reportService.paymentLedger(currentMonth, null, null, null, null, null, 0, 20);
        assertThat(ledger.content()).isEmpty();
        assertThat(ledger.totalElements()).isZero();

        PageResponse<AssetLedgerRow> assets = reportService.assetLedger(currentMonth, null, null, 0, 20);
        assertThat(assets.content()).isEmpty();
        assertThat(assets.totalElements()).isZero();

        PageResponse<AuditLogDTO> audit = reportService.auditTrail(currentMonth, null, null, null, null, 0, 20);
        assertThat(audit.content()).isEmpty();

        AdminOversightResponse oversight = reportService.adminOversight();
        assertThat(oversight.recentTransactions()).isEmpty();
        assertThat(oversight.recentAuditActivity()).isEmpty();
        assertThat(oversight.topBuyersByValue()).isEmpty();
        assertThat(oversight.farmerActivity()).isEmpty();
        assertThat(oversight.totalOutstandingPayments().total()).isEqualByComparingTo("0");
    }

    // ── Admin oversight metrics ──────────────────────────────────────────────

    @Test
    @DisplayName("Admin oversight exposes today's figures, exceptions and recent activity")
    void adminOversight_usesBackendAggregates() {
        authenticate(adminA);
        AdminOversightResponse response = reportService.adminOversight();

        assertThat(response.todayLots().total()).isEqualTo(1L);
        assertThat(response.todayTransactions().total()).isEqualTo(1L);
        assertThat(response.todayAuctionValue().total()).isEqualByComparingTo("10250.00");
        assertThat(response.todayPaymentCollection().total()).isEqualByComparingTo("7000.00");
        assertThat(response.totalOutstandingPayments().total()).isEqualByComparingTo("3250.00");
        assertThat(response.outstandingAssets().totalOutstanding()).isEqualTo(15L);
        assertThat(response.recentTransactions()).hasSize(1);
        assertThat(response.topBuyersByValue()).hasSize(1);
        assertThat(response.farmerActivity()).hasSize(1);
        assertThat(response.exceptions()).isNotEmpty();
        assertThat(response.generatedAt()).isNotNull();
    }

    // ── Audit trail ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("The audit trail captures the operational events, newest first")
    void auditTrail_capturesOperationalEvents() {
        authenticate(adminA);
        PageResponse<AuditLogDTO> page =
                reportService.auditTrail(currentMonth, null, null, null, null, 0, 50);

        assertThat(page.content()).isNotEmpty();
        assertThat(page.content()).extracting(AuditLogDTO::getAction).contains(
                "LOT_CREATED",
                "WEIGHMENT_RECORDED",
                "BID_RECORDED",
                "AUCTION_CLOSED",
                "WINNER_CONFIRMED",
                "TRANSACTION_CREATED",
                "PAYMENT_RECORDED",
                "ASSET_ISSUED",
                "ASSET_RETURNED");

        // newest-first ordering
        for (int i = 1; i < page.content().size(); i++) {
            assertThat(page.content().get(i - 1).getTimestamp())
                    .isAfterOrEqualTo(page.content().get(i).getTimestamp());
        }

        AuditLogDTO latest = page.content().get(0);
        assertThat(latest.getActorUsername()).isEqualTo("agent.pune");
        assertThat(latest.getActorRole()).isEqualTo("AGENT");
        assertThat(latest.getMarketId()).isEqualTo(marketA.getId());
        assertThat(latest.getEntityId()).isNotNull();
    }

    @Test
    @DisplayName("Audit rows never expose credentials")
    void auditTrail_excludesCredentials() {
        authenticate(adminA);
        List<AuditLogDTO> page = reportService.auditTrail(currentMonth, null, null, null, null, 0, 50).content();

        assertThat(page).allSatisfy(row -> {
            String value = row.getNewValue() == null ? "" : row.getNewValue();
            assertThat(value).doesNotContain("password");
            assertThat(value).doesNotContain("Bearer");
            assertThat(value).doesNotContain("test-hash");
        });
    }

    @Test
    @DisplayName("Audit filters narrow by action, entity type and actor")
    void auditTrail_filters() {
        authenticate(adminA);

        assertThat(reportService.auditTrail(currentMonth, null, "LOT_CREATED", null, null, 0, 50).content())
                .isNotEmpty();
        assertThat(reportService.auditTrail(currentMonth, null, "NOT_A_REAL_ACTION", null, null, 0, 50).content())
                .isEmpty();
        assertThat(reportService.auditTrail(currentMonth, null, null, "LOT", null, 0, 50).content())
                .isNotEmpty();
        assertThat(reportService.auditTrail(currentMonth, null, null, "PAYMENT", null, 0, 50).content())
                .isNotEmpty();
        assertThat(reportService.auditTrail(currentMonth, agentA.getId(), null, null, null, 0, 50).content())
                .isNotEmpty();
        assertThat(reportService.auditTrail(currentMonth, UUID.randomUUID(), null, null, null, 0, 50).content())
                .isEmpty();
    }

    @Test
    @DisplayName("The audit trail is isolated per market")
    void auditTrail_isMarketIsolated() {
        authenticate(agentB);
        List<AuditLogDTO> rows = reportService.auditTrail(currentMonth, null, null, null, null, 0, 50).content();

        assertThat(rows).isNotEmpty();
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getMarketId()).isEqualTo(marketB.getId());
            assertThat(row.getActorUsername()).isEqualTo("agent.nashik");
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void runFullScenario(User operator, Farmer farmer, Market market, Commodity commodity,
                                 AssetType assetType, Buyer lowBidder, Buyer highBidder, Buyer midBidder,
                                 BigDecimal gross, BigDecimal tare,
                                 BigDecimal lowBid, BigDecimal highBid, BigDecimal midBid,
                                 Buyer winner, BigDecimal paymentAmount, int cratesIssued, int cratesReturned) {

        authenticate(operator);

        LotRequestDTO lotRequest = new LotRequestDTO();
        lotRequest.setFarmerId(farmer.getId());
        lotRequest.setCommodityId(commodity.getId());
        if (assetType != null) {
            lotRequest.setAssetTypeId(assetType.getId());
        }
        Lot lot = lotService.createLot(lotRequest);

        WeighmentRequestDTO weighment = new WeighmentRequestDTO();
        weighment.setGrossWeight(gross);
        weighment.setTareWeight(tare);
        weighment.setWeightUnit("KG");
        lotService.recordWeighment(lot.getId(), weighment);

        auctionService.startAuction(lot.getId());
        BidResponseDTO low = auctionService.recordBid(lot.getId(), bid(lowBidder, lowBid));
        BidResponseDTO high = auctionService.recordBid(lot.getId(), bid(highBidder, highBid));
        auctionService.recordBid(lot.getId(), bid(midBidder, midBid));
        auctionService.closeAuction(lot.getId());

        WinnerConfirmationDTO winnerRequest = new WinnerConfirmationDTO();
        winnerRequest.setWinningBidId(high.getId());
        TransactionResponseDTO transaction = auctionService.confirmWinner(lot.getId(), winnerRequest);

        if (paymentAmount.signum() > 0) {
            PaymentRequestDTO payment = new PaymentRequestDTO();
            payment.setAmount(paymentAmount);
            payment.setPaymentMethod("CASH");
            paymentService.recordPayment(transaction.getId(), payment);
        }

        if (assetType != null && cratesIssued > 0) {
            AssetIssueReturnDTO issue = new AssetIssueReturnDTO();
            issue.setAssetTypeId(assetType.getId());
            issue.setQuantity(cratesIssued);
            assetService.issueAsset(transaction.getId(), issue);
        }
        if (assetType != null && cratesReturned > 0) {
            AssetIssueReturnDTO returned = new AssetIssueReturnDTO();
            returned.setAssetTypeId(assetType.getId());
            returned.setQuantity(cratesReturned);
            assetService.returnAsset(transaction.getId(), returned);
        }

        // Silence unused-variable warnings while keeping the low bid meaningful.
        assertThat(low.getId()).isNotNull();
        assertThat(market).isNotNull();
    }

    private BidRequestDTO bid(Buyer buyer, BigDecimal amount) {
        BidRequestDTO request = new BidRequestDTO();
        request.setBuyerId(buyer.getId());
        request.setBidAmount(amount);
        request.setPriceUnit("KG");
        return request;
    }

    private boolean denied(Runnable action) {
        try {
            action.run();
            return false;
        } catch (com.agrimandi.api.exception.BusinessValidationException ex) {
            return true;
        }
    }

    private void authenticate(User user) {
        CustomUserDetails details = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    private Market market(String name, String code) {
        Market market = new Market();
        market.setName(name);
        market.setCode(code);
        market.setStatus("ACTIVE");
        return marketRepository.save(market);
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

    private Farmer farmer(String name, Market market) {
        Farmer farmer = new Farmer();
        farmer.setName(name);
        farmer.setFarmerCode("F-" + UUID.randomUUID().toString().substring(0, 6));
        farmer.setMarket(market);
        farmer.setStatus("ACTIVE");
        return farmerRepository.save(farmer);
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
