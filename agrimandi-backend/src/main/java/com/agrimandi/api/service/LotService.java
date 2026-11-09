package com.agrimandi.api.service;

import com.agrimandi.api.dto.LotRequestDTO;
import com.agrimandi.api.entity.AssetType;
import com.agrimandi.api.entity.Commodity;
import com.agrimandi.api.entity.Farmer;
import com.agrimandi.api.entity.Lot;
import com.agrimandi.api.entity.Market;
import com.agrimandi.api.exception.BusinessValidationException;
import com.agrimandi.api.exception.ResourceNotFoundException;
import com.agrimandi.api.repository.AssetTypeRepository;
import com.agrimandi.api.repository.CommodityRepository;
import com.agrimandi.api.repository.FarmerRepository;
import com.agrimandi.api.repository.LotRepository;
import com.agrimandi.api.repository.MarketRepository;
import com.agrimandi.api.repository.WeighmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import com.agrimandi.api.security.SecurityUtils;
import com.agrimandi.api.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class LotService {

    private final LotRepository lotRepository;
    private final MarketRepository marketRepository;
    private final FarmerRepository farmerRepository;
    private final CommodityRepository commodityRepository;
    private final AssetTypeRepository assetTypeRepository;
    private final WeighmentRepository weighmentRepository;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final com.agrimandi.api.repository.TransactionRepository transactionRepository;
    private final AuditService auditService;

    @Transactional
    public Lot createLot(LotRequestDTO request) {
        UUID marketId = securityUtils.getCurrentMarketId();
        if ("ADMIN".equals(securityUtils.getCurrentUserRole()) && request.getMarketId() != null) {
            marketId = request.getMarketId();
        }

        if (marketId == null) {
            throw new BusinessValidationException("User is not associated with a market");
        }

        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new ResourceNotFoundException("Market not found"));

        Farmer farmer = farmerRepository.findById(request.getFarmerId())
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found"));

        if (!"ACTIVE".equals(farmer.getStatus())) {
            throw new BusinessValidationException("Farmer is not active");
        }

        if (farmer.getMarket() != null && !farmer.getMarket().getId().equals(marketId)) {
            throw new BusinessValidationException("Farmer does not belong to your market");
        }

        Commodity commodity = commodityRepository.findById(request.getCommodityId())
                .orElseThrow(() -> new ResourceNotFoundException("Commodity not found"));

        if (!Boolean.TRUE.equals(commodity.getActiveStatus())) {
            throw new BusinessValidationException("Commodity is not active");
        }

        Lot lot = new Lot();
        long lotCount = lotRepository.count() + 1;
        lot.setLotNumber(String.format("LOT-%d-%06d", java.time.LocalDate.now().getYear(), lotCount));
        lot.setMarket(market);
        lot.setFarmer(farmer);
        lot.setCommodity(commodity);
        lot.setQuantity(java.math.BigDecimal.ZERO);
        lot.setQuantityUnit(commodity.getDefaultPriceUnit() != null ? commodity.getDefaultPriceUnit() : "KG");
        lot.setPackagingType(request.getPackagingType());

        if (request.getAssetTypeId() != null) {
            AssetType assetType = assetTypeRepository.findById(request.getAssetTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Asset type not found"));
            lot.setAssetType(assetType);
        }

        userRepository.findById(securityUtils.getCurrentUserId()).ifPresent(lot::setCreatedBy);

        lot.setStatus("PENDING_WEIGHMENT");
        Lot savedLot = lotRepository.save(lot);
        auditService.logEvent(AuditService.LOT_CREATED, AuditService.ENTITY_LOT, savedLot.getId(), marketId);
        return savedLot;
    }

    @Transactional(readOnly = true)
    public java.util.List<Lot> getAllLots() {
        UUID marketId = securityUtils.getCurrentMarketId();
        String role = securityUtils.getCurrentUserRole();
        if ("ADMIN".equals(role)) {
            return lotRepository.findAll();
        }
        
        java.util.List<Lot> marketLots = lotRepository.findByMarketId(marketId);
        
        if ("FARMER".equals(role)) {
            UUID farmerId = securityUtils.getCurrentUserDetails().getFarmerId();
            return marketLots.stream().filter(l -> l.getFarmer() != null && l.getFarmer().getId().equals(farmerId)).collect(java.util.stream.Collectors.toList());
        }
        if ("BUYER".equals(role)) {
            UUID buyerId = securityUtils.getCurrentUserDetails().getBuyerId();
            java.util.List<com.agrimandi.api.entity.Transaction> buyerTxns = transactionRepository.findByBuyerId(buyerId);
            java.util.Set<UUID> buyerLotIds = buyerTxns.stream().map(t -> t.getLot().getId()).collect(java.util.stream.Collectors.toSet());
            return marketLots.stream().filter(l -> buyerLotIds.contains(l.getId())).collect(java.util.stream.Collectors.toList());
        }
        
        return marketLots;
    }

    @Transactional(readOnly = true)
    public Lot getLot(UUID lotId) {
        Lot lot = lotRepository.findById(lotId).orElseThrow(() -> new ResourceNotFoundException("Lot not found"));
        String role = securityUtils.getCurrentUserRole();
        
        if ("ADMIN".equals(role)) return lot;
        
        if (!lot.getMarket().getId().equals(securityUtils.getCurrentMarketId())) {
            throw new BusinessValidationException("Lot does not belong to your market");
        }
        
        if ("FARMER".equals(role)) {
            UUID farmerId = securityUtils.getCurrentUserDetails().getFarmerId();
            if (lot.getFarmer() == null || !lot.getFarmer().getId().equals(farmerId)) {
                throw new com.agrimandi.api.exception.BusinessValidationException("Not authorized to view this lot");
            }
        }

        if ("BUYER".equals(role)) {
            // A buyer may only see a lot they actually transacted on, which is the same rule
            // getAllLots applies. Without this, a buyer could read any lot in the market by id.
            UUID buyerId = securityUtils.getCurrentUserDetails().getBuyerId();
            boolean ownsLot = buyerId != null && transactionRepository.findByBuyerId(buyerId).stream()
                    .anyMatch(t -> t.getLot().getId().equals(lot.getId()));
            if (!ownsLot) {
                throw new com.agrimandi.api.exception.BusinessValidationException("Not authorized to view this lot");
            }
        }
        
        return lot;
    }

    @Transactional
    public com.agrimandi.api.entity.Weighment recordWeighment(UUID lotId, com.agrimandi.api.dto.WeighmentRequestDTO request) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Lot not found"));
                
        UUID marketId = securityUtils.getCurrentMarketId();
        if (!"ADMIN".equals(securityUtils.getCurrentUserRole()) && !lot.getMarket().getId().equals(marketId)) {
            throw new BusinessValidationException("Lot does not belong to your market");
        }
        
        if (request.getTareWeight().compareTo(request.getGrossWeight()) > 0) {
            throw new BusinessValidationException("Tare weight cannot be greater than gross weight");
        }
        
        java.math.BigDecimal netWeight = request.getGrossWeight().subtract(request.getTareWeight());
        if (netWeight.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("Net weight must be greater than zero");
        }
        
        com.agrimandi.api.entity.Weighment weighment = new com.agrimandi.api.entity.Weighment();
        weighment.setLot(lot);
        weighment.setGrossWeight(request.getGrossWeight());
        weighment.setTareWeight(request.getTareWeight());
        weighment.setNetWeight(netWeight);
        weighment.setWeightUnit(request.getWeightUnit());
        weighment.setWeighmentTime(java.time.OffsetDateTime.now());
        
        userRepository.findById(securityUtils.getCurrentUserId()).ifPresent(weighment::setRecordedBy);
        
        // Update lot quantity with the validated net weight
        lot.setQuantity(netWeight);
        lot.setQuantityUnit(request.getWeightUnit());
        lot.setStatus("READY_FOR_AUCTION");
        lotRepository.save(lot);

        com.agrimandi.api.entity.Weighment saved = weighmentRepository.save(weighment);
        auditService.logEvent(AuditService.WEIGHMENT_RECORDED, AuditService.ENTITY_LOT, lot.getId(),
                lot.getMarket().getId(),
                "netWeight=" + netWeight.toPlainString() + " " + request.getWeightUnit());
        return saved;
    }
}
