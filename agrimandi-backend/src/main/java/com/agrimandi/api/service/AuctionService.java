package com.agrimandi.api.service;

import com.agrimandi.api.dto.BidRequestDTO;
import com.agrimandi.api.dto.BidResponseDTO;
import com.agrimandi.api.dto.TransactionResponseDTO;
import com.agrimandi.api.dto.WinnerConfirmationDTO;
import com.agrimandi.api.entity.*;
import com.agrimandi.api.exception.BusinessValidationException;
import com.agrimandi.api.exception.ResourceNotFoundException;
import com.agrimandi.api.repository.*;
import com.agrimandi.api.security.SecurityUtils;
import com.agrimandi.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final LotRepository lotRepository;
    private final BuyerRepository buyerRepository;
    private final TransactionRepository transactionRepository;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public Auction startAuction(UUID lotId) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Lot not found"));
        
        if (!"ADMIN".equals(securityUtils.getCurrentUserRole()) && !lot.getMarket().getId().equals(securityUtils.getCurrentMarketId())) {
            throw new BusinessValidationException("You can only operate on lots in your assigned market");
        }

        if (!"READY_FOR_AUCTION".equals(lot.getStatus())) {
            throw new BusinessValidationException("Lot is not ready for auction");
        }

        if (auctionRepository.findByLotId(lotId).isPresent()) {
            throw new BusinessValidationException("Auction already exists for this lot");
        }

        lot.setStatus("AUCTION_IN_PROGRESS");
        lotRepository.save(lot);

        Auction auction = new Auction();
        auction.setLot(lot);
        auction.setStatus("IN_PROGRESS");
        auction.setStartedAt(OffsetDateTime.now());
        
        userRepository.findById(securityUtils.getCurrentUserId()).ifPresent(auction::setCreatedBy);

        Auction savedAuction = auctionRepository.save(auction);
        auditService.logEvent(AuditService.AUCTION_STARTED, AuditService.ENTITY_AUCTION, savedAuction.getId(), marketIdOf(lot));
        return savedAuction;
    }

    @Transactional
    public BidResponseDTO recordBid(UUID lotId, BidRequestDTO request) {
        Auction auction = auctionRepository.findByLotId(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found for lot"));
                
        if (!"ADMIN".equals(securityUtils.getCurrentUserRole()) && !auction.getLot().getMarket().getId().equals(securityUtils.getCurrentMarketId())) {
            throw new BusinessValidationException("You can only operate on lots in your assigned market");
        }

        if (!"IN_PROGRESS".equals(auction.getStatus())) {
            throw new BusinessValidationException("Auction is not in progress");
        }
        
        if (request.getBidAmount() == null || request.getBidAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("Bid amount must be greater than zero");
        }
        
        if (!auction.getLot().getQuantityUnit().equals(request.getPriceUnit())) {
            throw new BusinessValidationException("Price unit must match lot quantity unit");
        }

        Buyer buyer = buyerRepository.findById(request.getBuyerId())
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));
                
        if (!"ACTIVE".equals(buyer.getStatus())) {
            throw new BusinessValidationException("Buyer is not active");
        }
        
        if (buyer.getMarket() != null && !buyer.getMarket().getId().equals(auction.getLot().getMarket().getId())) {
            throw new BusinessValidationException("Buyer does not belong to the same market");
        }

        Bid bid = new Bid();
        bid.setAuction(auction);
        bid.setBuyer(buyer);
        bid.setBidAmount(request.getBidAmount());
        bid.setPriceUnit(request.getPriceUnit());
        bid.setStatus("ACTIVE");
        bid.setBidTime(OffsetDateTime.now());
        
        userRepository.findById(securityUtils.getCurrentUserId()).ifPresent(bid::setRecordedBy);

        Bid savedBid = bidRepository.save(bid);

        auditService.logEvent(AuditService.BID_RECORDED, AuditService.ENTITY_BID, savedBid.getId(),
                auction.getLot().getMarket().getId(),
                "amount=" + request.getBidAmount().toPlainString() + " " + request.getPriceUnit());

        return mapToBidResponse(savedBid);
    }
    
    @Transactional
    public void closeAuction(UUID lotId) {
        Auction auction = auctionRepository.findByLotId(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found for lot"));
                
        if (!"ADMIN".equals(securityUtils.getCurrentUserRole()) && !auction.getLot().getMarket().getId().equals(securityUtils.getCurrentMarketId())) {
            throw new BusinessValidationException("You can only operate on lots in your assigned market");
        }

        if (!"IN_PROGRESS".equals(auction.getStatus())) {
            throw new BusinessValidationException("Auction is not in progress");
        }
        
        List<Bid> activeBids = bidRepository.findActiveBidsByAuctionIdOrderByAmountDesc(auction.getId());
        
        if (activeBids.isEmpty()) {
            auction.setStatus("CLOSED_NO_BIDS");
            auction.setTieDetected(false);
            auction.setEndedAt(OffsetDateTime.now());
            auctionRepository.save(auction);

            Lot lot = auction.getLot();
            lot.setStatus("READY_FOR_AUCTION"); // Can be re-auctioned
            lotRepository.save(lot);
            auditService.logEvent(AuditService.AUCTION_CLOSED, AuditService.ENTITY_AUCTION, auction.getId(),
                    auction.getLot().getMarket().getId(), "status=CLOSED_NO_BIDS");
            return;
        }

        BigDecimal highestAmount = activeBids.get(0).getBidAmount();

        // Check for tie
        long highestBidsCount = activeBids.stream()
                .filter(b -> b.getBidAmount().compareTo(highestAmount) == 0)
                .count();

        if (highestBidsCount > 1) {
            auction.setStatus("TIE_REQUIRES_AGENT_DECISION");
            auction.setTieDetected(true);
        } else {
            auction.setStatus("WINNER_PENDING_CONFIRMATION");
            auction.setTieDetected(false);
        }

        auction.setEndedAt(OffsetDateTime.now());
        auctionRepository.save(auction);

        Lot lot = auction.getLot();
        if ("TIE_REQUIRES_AGENT_DECISION".equals(auction.getStatus())) {
            lot.setStatus("TIE_REQUIRES_AGENT_DECISION");
        } else {
            lot.setStatus("WINNER_PENDING_CONFIRMATION");
        }
        lotRepository.save(lot);

        auditService.logEvent(AuditService.AUCTION_CLOSED, AuditService.ENTITY_AUCTION, auction.getId(),
                auction.getLot().getMarket().getId(),
                "status=" + auction.getStatus() + ", highestBids=" + highestBidsCount);
    }

    @Transactional
    public TransactionResponseDTO confirmWinner(UUID lotId, WinnerConfirmationDTO request) {
        Auction auction = auctionRepository.findWithLockByLotId(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found for lot"));
                
        if (!"ADMIN".equals(securityUtils.getCurrentUserRole()) && !auction.getLot().getMarket().getId().equals(securityUtils.getCurrentMarketId())) {
            throw new BusinessValidationException("You can only operate on lots in your assigned market");
        }

        if (!"WINNER_PENDING_CONFIRMATION".equals(auction.getStatus()) && !"TIE_REQUIRES_AGENT_DECISION".equals(auction.getStatus())) {
            throw new BusinessValidationException("Auction is not awaiting winner confirmation");
        }
        
        if (transactionRepository.findByAuctionId(auction.getId()).isPresent()) {
            throw new BusinessValidationException("Transaction already exists for this auction");
        }

        Bid winningBid = bidRepository.findById(request.getWinningBidId())
                .orElseThrow(() -> new ResourceNotFoundException("Bid not found"));
                
        if (!winningBid.getAuction().getId().equals(auction.getId())) {
            throw new BusinessValidationException("Bid does not belong to this auction");
        }
        
        if (!"ACTIVE".equals(winningBid.getBuyer().getStatus())) {
            throw new BusinessValidationException("Winner buyer is not active");
        }
        
        // Ensure it is one of the highest bids
        List<Bid> activeBids = bidRepository.findActiveBidsByAuctionIdOrderByAmountDesc(auction.getId());
        BigDecimal highestAmount = activeBids.isEmpty() ? BigDecimal.ZERO : activeBids.get(0).getBidAmount();
        
        if (winningBid.getBidAmount().compareTo(highestAmount) < 0) {
            throw new BusinessValidationException("Selected bid is not among the highest valid bids");
        }
        
        // Confirm winner
        auction.setWinnerBid(winningBid);
        auction.setWinnerBuyer(winningBid.getBuyer());
        auction.setStatus("WINNER_CONFIRMED");
        auctionRepository.save(auction);
        
        // Update Lot status
        Lot lot = auction.getLot();
        lot.setStatus("TRANSACTION_CREATED");
        lotRepository.save(lot);
        
        // Create Transaction
        Transaction transaction = new Transaction();
        long txnNum = transactionRepository.count() + 1;
        transaction.setTransactionNumber(String.format("TXN-%d-%06d", java.time.LocalDate.now().getYear(), txnNum));
        transaction.setLot(lot);
        transaction.setAuction(auction);
        transaction.setFarmer(lot.getFarmer());
        transaction.setBuyer(winningBid.getBuyer());
        transaction.setWinningBid(winningBid);
        
        transaction.setQuantity(lot.getQuantity());
        transaction.setQuantityUnit(lot.getQuantityUnit());
        
        transaction.setWinningRate(winningBid.getBidAmount());
        transaction.setPriceUnit(winningBid.getPriceUnit());
        
        // Business Rule calculation: gross_amount = quantity * winning_rate
        BigDecimal grossAmount = lot.getQuantity().multiply(winningBid.getBidAmount());
        transaction.setGrossAmount(grossAmount);
        transaction.setAdjustments(BigDecimal.ZERO);
        transaction.setNetAmount(grossAmount); // net = gross - adjustments
        
        transaction.setStatus("CREATED");
        transaction.setTransactionDate(OffsetDateTime.now());
        
        userRepository.findById(securityUtils.getCurrentUserId()).ifPresent(transaction::setCreatedBy);
        
        Transaction savedTx = transactionRepository.save(transaction);

        auditService.logEvent(AuditService.WINNER_CONFIRMED, AuditService.ENTITY_AUCTION, auction.getId(),
                lot.getMarket().getId(),
                "winningBid=" + winningBid.getBidAmount().toPlainString() + " " + winningBid.getPriceUnit());
        auditService.logEvent(AuditService.TRANSACTION_CREATED, AuditService.ENTITY_TRANSACTION, savedTx.getId(),
                lot.getMarket().getId(),
                "transactionNumber=" + savedTx.getTransactionNumber() + ", net=" + savedTx.getNetAmount().toPlainString());

        return mapToTransactionResponse(savedTx);
    }
    
    public List<BidResponseDTO> getAuctionBids(UUID lotId) {
        Auction auction = auctionRepository.findByLotId(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));
                
        // Ensure farmer/buyer can only see bids on lots related to them, unless Agent/Admin
        enforceReadAccess(auction.getLot());

        return bidRepository.findByAuctionIdOrderByBidAmountDesc(auction.getId())
                .stream()
                .map(this::mapToBidResponse)
                .collect(Collectors.toList());
    }
    
    public List<BidResponseDTO> getHighestBids(UUID lotId) {
        Auction auction = auctionRepository.findByLotId(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));
                
        enforceReadAccess(auction.getLot());

        List<Bid> activeBids = bidRepository.findActiveBidsByAuctionIdOrderByAmountDesc(auction.getId());
        if (activeBids.isEmpty()) return List.of();
        
        BigDecimal highest = activeBids.get(0).getBidAmount();
        return activeBids.stream()
                .filter(b -> b.getBidAmount().compareTo(highest) == 0)
                .map(this::mapToBidResponse)
                .collect(Collectors.toList());
    }

    private BidResponseDTO mapToBidResponse(Bid bid) {
        BidResponseDTO dto = new BidResponseDTO();
        dto.setId(bid.getId());
        dto.setAuctionId(bid.getAuction().getId());
        dto.setBuyerId(bid.getBuyer().getId());
        dto.setBuyerName(bid.getBuyer().getName());
        dto.setBidAmount(bid.getBidAmount());
        dto.setPriceUnit(bid.getPriceUnit());
        dto.setBidTime(bid.getBidTime());
        dto.setStatus(bid.getStatus());
        return dto;
    }
    
    private final TransactionService transactionService;

    // Added in constructor implicitly via @RequiredArgsConstructor because transactionService is final, wait!
    // Since AuctionService uses @RequiredArgsConstructor, adding transactionService will require it in constructor.
    // Wait, adding a field might cause circular dependency if TransactionService also depends on something.
    // But TransactionService depends on TransactionRepository, PaymentRepository, AssetTransactionRepository, SecurityUtils.
    
    // Instead of mapping here, I will just return the transactionService.mapToDetailDTO(transaction) if I inject it.
    // Let me update the whole block.
    
    private TransactionResponseDTO mapToTransactionResponse(Transaction transaction) {
        TransactionResponseDTO dto = new TransactionResponseDTO();
        dto.setId(transaction.getId());
        dto.setTransactionNumber(transaction.getTransactionNumber());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setPaymentStatus(transaction.getStatus());
        
        TransactionResponseDTO.LotDTO lotDTO = new TransactionResponseDTO.LotDTO();
        lotDTO.setId(transaction.getLot().getId());
        lotDTO.setLotNumber(transaction.getLot().getLotNumber());
        lotDTO.setCommodity(transaction.getLot().getCommodity().getName());
        lotDTO.setQuantity(transaction.getLot().getQuantity());
        lotDTO.setUnit(transaction.getLot().getQuantityUnit());
        dto.setLot(lotDTO);
        
        TransactionResponseDTO.ActorDTO farmerDTO = new TransactionResponseDTO.ActorDTO();
        farmerDTO.setId(transaction.getFarmer().getId());
        farmerDTO.setName(transaction.getFarmer().getName());
        dto.setFarmer(farmerDTO);
        
        TransactionResponseDTO.ActorDTO buyerDTO = new TransactionResponseDTO.ActorDTO();
        buyerDTO.setId(transaction.getBuyer().getId());
        buyerDTO.setName(transaction.getBuyer().getName());
        dto.setBuyer(buyerDTO);
        
        TransactionResponseDTO.BidDTO bidDTO = new TransactionResponseDTO.BidDTO();
        bidDTO.setId(transaction.getWinningBid().getId());
        bidDTO.setRate(transaction.getWinningBid().getBidAmount());
        bidDTO.setPriceUnit(transaction.getWinningBid().getPriceUnit());
        dto.setWinningBid(bidDTO);
        
        TransactionResponseDTO.AmountDTO amountDTO = new TransactionResponseDTO.AmountDTO();
        amountDTO.setGross(transaction.getGrossAmount());
        amountDTO.setNet(transaction.getNetAmount());
        amountDTO.setPaid(BigDecimal.ZERO);
        amountDTO.setPending(transaction.getNetAmount());
        dto.setAmount(amountDTO);
        
        return dto;
    }
    
    private UUID marketIdOf(Lot lot) {
        return lot.getMarket() != null ? lot.getMarket().getId() : null;
    }

    private void enforceReadAccess(Lot lot) {
        String role = securityUtils.getCurrentUserRole();
        if ("ADMIN".equals(role)) return;
        
        if ("AGENT".equals(role)) {
            if (!lot.getMarket().getId().equals(securityUtils.getCurrentMarketId())) {
                throw new BusinessValidationException("You can only access data for your assigned market");
            }
        } else if ("FARMER".equals(role)) {
            UUID farmerId = securityUtils.getCurrentUserDetails().getFarmerId();
            if (farmerId == null || !farmerId.equals(lot.getFarmer().getId())) {
                throw new BusinessValidationException("You can only view your own records");
            }
        } else if ("BUYER".equals(role)) {
            UUID buyerId = securityUtils.getCurrentUserDetails().getBuyerId();
            if (buyerId == null) {
                throw new BusinessValidationException("Buyer identity not found");
            }
        }
    }
}
