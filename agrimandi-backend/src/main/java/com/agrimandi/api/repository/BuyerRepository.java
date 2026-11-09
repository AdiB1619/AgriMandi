package com.agrimandi.api.repository;

import com.agrimandi.api.entity.Buyer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BuyerRepository extends JpaRepository<Buyer, UUID> {
    Page<Buyer> findByMarketId(UUID marketId, Pageable pageable);
}
