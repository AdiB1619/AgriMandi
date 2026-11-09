package com.agrimandi.api.service;

import com.agrimandi.api.dto.TransactionResponseDTO;
import com.agrimandi.api.entity.Transaction;
import com.agrimandi.api.exception.BusinessValidationException;
import com.agrimandi.api.exception.ResourceNotFoundException;
import com.agrimandi.api.repository.AssetTransactionRepository;
import com.agrimandi.api.repository.PaymentRepository;
import com.agrimandi.api.repository.TransactionRepository;
import com.agrimandi.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final AssetTransactionRepository assetTransactionRepository;
    private final SecurityUtils securityUtils;

    public TransactionResponseDTO getTransactionDetail(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        enforceReadAccess(transaction);
        
        return mapToDetailDTO(transaction);
    }
    
    public TransactionResponseDTO mapToDetailDTO(Transaction transaction) {
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
        
        BigDecimal totalPaid = paymentRepository.sumAmountByTransactionId(transaction.getId());
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        
        TransactionResponseDTO.AmountDTO amountDTO = new TransactionResponseDTO.AmountDTO();
        amountDTO.setGross(transaction.getGrossAmount());
        amountDTO.setNet(transaction.getNetAmount());
        amountDTO.setPaid(totalPaid);
        amountDTO.setPending(transaction.getNetAmount().subtract(totalPaid));
        dto.setAmount(amountDTO);
        
        if (transaction.getLot().getAssetType() != null) {
            TransactionResponseDTO.AssetSummaryDTO assetDTO = new TransactionResponseDTO.AssetSummaryDTO();
            assetDTO.setAssetTypeId(transaction.getLot().getAssetType().getId());
            assetDTO.setAssetTypeName(transaction.getLot().getAssetType().getName());
            
            Integer issued = assetTransactionRepository.sumQuantityByMovementType(transaction.getId(), transaction.getLot().getAssetType().getId(), "ISSUED");
            if (issued == null) issued = 0;
            
            Integer returned = assetTransactionRepository.sumQuantityByMovementType(transaction.getId(), transaction.getLot().getAssetType().getId(), "RETURNED");
            if (returned == null) returned = 0;
            
            assetDTO.setIssued(issued);
            assetDTO.setReturned(returned);
            assetDTO.setOutstanding(issued - returned);
            dto.setAssets(assetDTO);
        }
        
        return dto;
    }
    
    private void enforceReadAccess(Transaction tx) {
        String role = securityUtils.getCurrentUserRole();
        if ("ADMIN".equals(role)) return;
        
        if ("AGENT".equals(role)) {
            if (!tx.getLot().getMarket().getId().equals(securityUtils.getCurrentMarketId())) {
                throw new BusinessValidationException("You can only access data for your assigned market");
            }
        } else if ("FARMER".equals(role)) {
            UUID farmerId = securityUtils.getCurrentUserDetails().getFarmerId();
            if (farmerId == null || !farmerId.equals(tx.getFarmer().getId())) {
                throw new BusinessValidationException("You can only view your own records");
            }
        } else if ("BUYER".equals(role)) {
            UUID buyerId = securityUtils.getCurrentUserDetails().getBuyerId();
            if (buyerId == null || !buyerId.equals(tx.getBuyer().getId())) {
                throw new BusinessValidationException("You can only view your own records");
            }
        }
    }
}
