package com.agrimandi.api.service;

import com.agrimandi.api.dto.BidRequestDTO;
import com.agrimandi.api.dto.BidResponseDTO;
import com.agrimandi.api.dto.TransactionResponseDTO;
import com.agrimandi.api.dto.WinnerConfirmationDTO;
import com.agrimandi.api.entity.*;
import com.agrimandi.api.exception.BusinessValidationException;
import com.agrimandi.api.repository.*;
import com.agrimandi.api.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock private AuctionRepository auctionRepository;
    @Mock private BidRepository bidRepository;
    @Mock private LotRepository lotRepository;
    @Mock private BuyerRepository buyerRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private AuctionService auctionService;

    private Lot mockLot;
    private Auction mockAuction;
    private Buyer mockBuyer;
    private Bid mockBid1;
    private Bid mockBid2;
    private Farmer mockFarmer;
    
    @BeforeEach
    void setUp() {
        mockFarmer = new Farmer();
        mockFarmer.setId(UUID.randomUUID());
        
        Market mockMarket = new Market();
        mockMarket.setId(UUID.randomUUID());
        
        Commodity mockCommodity = new Commodity();
        mockCommodity.setId(UUID.randomUUID());
        mockCommodity.setName("Tomato");
        
        mockLot = new Lot();
        mockLot.setId(UUID.randomUUID());
        mockLot.setStatus("READY_FOR_AUCTION");
        mockLot.setFarmer(mockFarmer);
        mockLot.setMarket(mockMarket);
        mockLot.setCommodity(mockCommodity);
        mockLot.setQuantity(BigDecimal.valueOf(100));
        mockLot.setQuantityUnit("KG");

        mockAuction = new Auction();
        mockAuction.setId(UUID.randomUUID());
        mockAuction.setLot(mockLot);
        mockAuction.setStatus("IN_PROGRESS");

        mockBuyer = new Buyer();
        mockBuyer.setId(UUID.randomUUID());
        mockBuyer.setStatus("ACTIVE");

        mockBid1 = new Bid();
        mockBid1.setId(UUID.randomUUID());
        mockBid1.setAuction(mockAuction);
        mockBid1.setBuyer(mockBuyer);
        mockBid1.setBidAmount(BigDecimal.valueOf(20.50));
        mockBid1.setPriceUnit("KG");
        mockBid1.setStatus("ACTIVE");

        mockBid2 = new Bid();
        mockBid2.setId(UUID.randomUUID());
        mockBid2.setAuction(mockAuction);
        mockBid2.setBuyer(mockBuyer);
        mockBid2.setBidAmount(BigDecimal.valueOf(20.50)); // Tied amount
        mockBid2.setPriceUnit("KG");
        mockBid2.setStatus("ACTIVE");
    }

    @Test
    void recordBid_shouldSucceed() {
        BidRequestDTO request = new BidRequestDTO();
        request.setBuyerId(mockBuyer.getId());
        request.setBidAmount(BigDecimal.valueOf(20.50));
        request.setPriceUnit("KG");

        when(securityUtils.getCurrentUserRole()).thenReturn("AGENT");
        when(securityUtils.getCurrentMarketId()).thenReturn(mockLot.getMarket().getId());

        when(auctionRepository.findByLotId(mockLot.getId())).thenReturn(Optional.of(mockAuction));
        when(buyerRepository.findById(mockBuyer.getId())).thenReturn(Optional.of(mockBuyer));
        when(bidRepository.save(any(Bid.class))).thenReturn(mockBid1);

        BidResponseDTO response = auctionService.recordBid(mockLot.getId(), request);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(20.50), response.getBidAmount());
        verify(bidRepository).save(any(Bid.class));
    }

    @Test
    void closeAuction_withTie_shouldSetStatusToTie() {
        when(securityUtils.getCurrentUserRole()).thenReturn("AGENT");
        when(securityUtils.getCurrentMarketId()).thenReturn(mockLot.getMarket().getId());

        when(auctionRepository.findByLotId(mockLot.getId())).thenReturn(Optional.of(mockAuction));
        when(bidRepository.findActiveBidsByAuctionIdOrderByAmountDesc(mockAuction.getId()))
                .thenReturn(List.of(mockBid1, mockBid2)); // Two bids with 20.50

        auctionService.closeAuction(mockLot.getId());

        assertEquals("TIE_REQUIRES_AGENT_DECISION", mockAuction.getStatus());
        assertEquals("TIE_REQUIRES_AGENT_DECISION", mockLot.getStatus());
        verify(auctionRepository).save(mockAuction);
        verify(lotRepository).save(mockLot);
    }

    @Test
    void confirmWinner_shouldCreateTransaction() {
        mockAuction.setStatus("WINNER_PENDING_CONFIRMATION");
        
        WinnerConfirmationDTO request = new WinnerConfirmationDTO();
        request.setWinningBidId(mockBid1.getId());

        when(securityUtils.getCurrentUserRole()).thenReturn("AGENT");
        when(securityUtils.getCurrentMarketId()).thenReturn(mockLot.getMarket().getId());

        when(auctionRepository.findWithLockByLotId(mockLot.getId())).thenReturn(Optional.of(mockAuction));
        when(transactionRepository.findByAuctionId(mockAuction.getId())).thenReturn(Optional.empty());
        when(bidRepository.findById(mockBid1.getId())).thenReturn(Optional.of(mockBid1));
        when(bidRepository.findActiveBidsByAuctionIdOrderByAmountDesc(mockAuction.getId()))
                .thenReturn(List.of(mockBid1)); // mockBid1 is highest
                
        Transaction savedTx = new Transaction();
        savedTx.setId(UUID.randomUUID());
        savedTx.setLot(mockLot);
        savedTx.setAuction(mockAuction);
        savedTx.setFarmer(mockFarmer);
        savedTx.setBuyer(mockBuyer);
        savedTx.setWinningBid(mockBid1);
        savedTx.setGrossAmount(BigDecimal.valueOf(2050.00));
        savedTx.setNetAmount(BigDecimal.valueOf(2050.00));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

        TransactionResponseDTO response = auctionService.confirmWinner(mockLot.getId(), request);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(2050.00), response.getAmount().getGross());
        assertEquals("WINNER_CONFIRMED", mockAuction.getStatus());
        assertEquals("TRANSACTION_CREATED", mockLot.getStatus());
        
        verify(transactionRepository).save(any(Transaction.class));
    }
    
    @Test
    void confirmWinner_withInvalidBidAmount_shouldThrowException() {
        mockAuction.setStatus("WINNER_PENDING_CONFIRMATION");
        
        WinnerConfirmationDTO request = new WinnerConfirmationDTO();
        request.setWinningBidId(mockBid1.getId());
        
        Bid higherBid = new Bid();
        higherBid.setBidAmount(BigDecimal.valueOf(25.00));

        when(securityUtils.getCurrentUserRole()).thenReturn("AGENT");
        when(securityUtils.getCurrentMarketId()).thenReturn(mockLot.getMarket().getId());

        when(auctionRepository.findWithLockByLotId(mockLot.getId())).thenReturn(Optional.of(mockAuction));
        when(transactionRepository.findByAuctionId(mockAuction.getId())).thenReturn(Optional.empty());
        when(bidRepository.findById(mockBid1.getId())).thenReturn(Optional.of(mockBid1));
        when(bidRepository.findActiveBidsByAuctionIdOrderByAmountDesc(mockAuction.getId()))
                .thenReturn(List.of(higherBid, mockBid1)); // mockBid1 is NOT highest
                
        assertThrows(BusinessValidationException.class, () -> auctionService.confirmWinner(mockLot.getId(), request));
    }
}
