package com.agrimandi.api.repository;

import com.agrimandi.api.entity.Market;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MarketRepository extends JpaRepository<Market, UUID> {
    Optional<Market> findByCode(String code);
}
