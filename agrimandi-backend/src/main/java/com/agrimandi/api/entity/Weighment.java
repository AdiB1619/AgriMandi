package com.agrimandi.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "weighments")
@Getter
@Setter
public class Weighment extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "lot_id", nullable = false)
    private Lot lot;

    private BigDecimal grossWeight;
    private BigDecimal tareWeight;
    private BigDecimal netWeight;
    private String weightUnit;
    
    private OffsetDateTime weighmentTime;
    private String weighmentReference;

    @ManyToOne
    @JoinColumn(name = "recorded_by")
    private User recordedBy;
}
