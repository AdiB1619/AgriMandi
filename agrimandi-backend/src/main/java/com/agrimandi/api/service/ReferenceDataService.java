package com.agrimandi.api.service;

import com.agrimandi.api.dto.ReferenceDTOs.*;
import com.agrimandi.api.entity.*;
import com.agrimandi.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReferenceDataService {

    private final FarmerRepository farmerRepository;
    private final BuyerRepository buyerRepository;
    private final CommodityRepository commodityRepository;
    private final AssetTypeRepository assetTypeRepository;

    @Transactional(readOnly = true)
    public List<FarmerDTO> getFarmersByMarket(UUID marketId) {
        return farmerRepository.findByMarketId(marketId, org.springframework.data.domain.Pageable.unpaged()).stream()
                .map(this::mapToFarmerDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FarmerDTO getFarmerById(UUID id, UUID marketId) {
        Farmer farmer = farmerRepository.findById(id).orElseThrow(() -> new RuntimeException("Farmer not found"));
        if (!farmer.getMarket().getId().equals(marketId)) {
            throw new RuntimeException("Access denied");
        }
        return mapToFarmerDTO(farmer);
    }

    @Transactional(readOnly = true)
    public List<BuyerDTO> getBuyersByMarket(UUID marketId) {
        return buyerRepository.findByMarketId(marketId, org.springframework.data.domain.Pageable.unpaged()).stream()
                .map(this::mapToBuyerDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BuyerDTO getBuyerById(UUID id, UUID marketId) {
        Buyer buyer = buyerRepository.findById(id).orElseThrow(() -> new RuntimeException("Buyer not found"));
        if (!buyer.getMarket().getId().equals(marketId)) {
            throw new RuntimeException("Access denied");
        }
        return mapToBuyerDTO(buyer);
    }

    @Transactional(readOnly = true)
    public List<CommodityDTO> getAllCommodities() {
        return commodityRepository.findAll().stream()
                .map(this::mapToCommodityDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssetTypeDTO> getAssetTypesByMarket(UUID marketId) {
        return assetTypeRepository.findByMarketIdAndActiveTrue(marketId).stream()
                .map(this::mapToAssetTypeDTO)
                .collect(Collectors.toList());
    }

    private FarmerDTO mapToFarmerDTO(Farmer entity) {
        FarmerDTO dto = new FarmerDTO();
        dto.setId(entity.getId());
        dto.setFarmerCode(entity.getFarmerCode());
        dto.setName(entity.getName());
        dto.setVillage(entity.getVillage());
        dto.setPhone(entity.getPhone());
        dto.setAddress(entity.getAddress());
        dto.setStatus(entity.getStatus());
        return dto;
    }

    private BuyerDTO mapToBuyerDTO(Buyer entity) {
        BuyerDTO dto = new BuyerDTO();
        dto.setId(entity.getId());
        dto.setBuyerCode(entity.getBuyerCode());
        dto.setName(entity.getName());
        dto.setBusinessName(entity.getBusinessName());
        dto.setPhone(entity.getPhone());
        dto.setAddress(entity.getAddress());
        dto.setLicenseReference(entity.getLicenseReference());
        dto.setStatus(entity.getStatus());
        return dto;
    }

    private CommodityDTO mapToCommodityDTO(Commodity entity) {
        CommodityDTO dto = new CommodityDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setDefaultPriceUnit(entity.getDefaultPriceUnit());
        dto.setActiveStatus(entity.getActiveStatus());
        return dto;
    }

    private AssetTypeDTO mapToAssetTypeDTO(AssetType entity) {
        AssetTypeDTO dto = new AssetTypeDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setReturnable(entity.getReturnable());
        return dto;
    }
}
