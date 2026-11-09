package com.agrimandi.api.repository;

import com.agrimandi.api.entity.Weighment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WeighmentRepository extends JpaRepository<Weighment, UUID> {
    List<Weighment> findByLotId(UUID lotId);
}
