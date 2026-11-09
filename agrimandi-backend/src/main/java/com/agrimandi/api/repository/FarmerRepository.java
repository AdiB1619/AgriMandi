package com.agrimandi.api.repository;

import com.agrimandi.api.entity.Farmer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FarmerRepository extends JpaRepository<Farmer, UUID> {
    Page<Farmer> findByMarketId(UUID marketId, Pageable pageable);
}
