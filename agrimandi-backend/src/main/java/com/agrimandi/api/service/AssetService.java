package com.agrimandi.api.service;

import com.agrimandi.api.dto.AssetIssueReturnDTO;
import com.agrimandi.api.entity.AssetTransaction;
import com.agrimandi.api.entity.AssetType;
import com.agrimandi.api.entity.Transaction;
import com.agrimandi.api.exception.BusinessValidationException;
import com.agrimandi.api.exception.ResourceNotFoundException;
import com.agrimandi.api.repository.AssetTransactionRepository;
import com.agrimandi.api.repository.AssetTypeRepository;
import com.agrimandi.api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.agrimandi.api.security.SecurityUtils;
import com.agrimandi.api.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetTransactionRepository assetTransactionRepository;
    private final AssetTypeRepository assetTypeRepository;
    private final TransactionRepository transactionRepository;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public AssetTransaction issueAsset(UUID transactionId, AssetIssueReturnDTO request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BusinessValidationException("Issue quantity must be greater than zero");
        }
        
        Transaction transaction = getTransaction(transactionId);
        AssetType assetType = getAssetType(request.getAssetTypeId());

        return createAssetTransaction(transaction, assetType, request.getQuantity(), "ISSUED", request.getNotes());
    }
    @Transactional
    public AssetTransaction returnAsset(UUID transactionId, AssetIssueReturnDTO request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BusinessValidationException("Return quantity must be greater than zero");
        }
        
        Transaction transaction = getTransaction(transactionId);
        AssetType assetType = getAssetType(request.getAssetTypeId());

        Integer outstanding = assetTransactionRepository.calculateOutstandingQuantity(transactionId, assetType.getId());
        if (outstanding == null) outstanding = 0;

        if (request.getQuantity() > outstanding) {
            throw new BusinessValidationException("Return quantity exceeds outstanding issued quantity. Outstanding: " + outstanding);
        }

        return createAssetTransaction(transaction, assetType, request.getQuantity(), "RETURNED", request.getNotes());
    }

    private Transaction getTransaction(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
                
        if (!"ADMIN".equals(securityUtils.getCurrentUserRole()) && !transaction.getLot().getMarket().getId().equals(securityUtils.getCurrentMarketId())) {
            throw new BusinessValidationException("You can only operate on transactions in your assigned market");
        }
        
        return transaction;
    }

    private AssetType getAssetType(UUID assetTypeId) {
        AssetType assetType = assetTypeRepository.findById(assetTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset type not found"));
        if (!assetType.getReturnable()) {
            throw new BusinessValidationException("Asset type is not returnable");
        }
        return assetType;
    }

    private AssetTransaction createAssetTransaction(Transaction transaction, AssetType assetType, Integer quantity, String type, String notes) {
        AssetTransaction at = new AssetTransaction();
        at.setTransaction(transaction);
        at.setAssetType(assetType);
        at.setQuantity(quantity);
        at.setMovementType(type);
        at.setMovementDate(OffsetDateTime.now());
        at.setNotes(notes);
        
        userRepository.findById(securityUtils.getCurrentUserId()).ifPresent(at::setRecordedBy);

        AssetTransaction saved = assetTransactionRepository.save(at);

        boolean isIssue = "ISSUED".equals(type);
        auditService.logEvent(isIssue ? AuditService.ASSET_ISSUED : AuditService.ASSET_RETURNED,
                AuditService.ENTITY_ASSET_TRANSACTION, saved.getId(),
                transaction.getLot().getMarket().getId(),
                "transaction=" + transaction.getTransactionNumber()
                        + ", assetType=" + assetType.getName()
                        + ", quantity=" + quantity);

        return saved;
    }
}
