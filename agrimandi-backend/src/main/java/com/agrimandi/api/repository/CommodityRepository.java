package com.agrimandi.api.repository;

import com.agrimandi.api.entity.Commodity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CommodityRepository extends JpaRepository<Commodity, UUID> {
    Page<Commodity> findAllByActiveStatusTrue(Pageable pageable);
}
