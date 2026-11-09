package com.agrimandi.api.repository;

import com.agrimandi.api.entity.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssetTypeRepository extends JpaRepository<AssetType, UUID> {
    List<AssetType> findByMarketIdAndActiveTrue(UUID marketId);
}
