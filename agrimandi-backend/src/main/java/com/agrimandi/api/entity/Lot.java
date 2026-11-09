package com.agrimandi.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "lots")
@Getter
@Setter
public class Lot extends BaseEntity {

    private String lotNumber;

    @ManyToOne
    @JoinColumn(name = "market_id", nullable = false)
    private Market market;

    @ManyToOne
    @JoinColumn(name = "farmer_id", nullable = false)
    private Farmer farmer;

    @ManyToOne
    @JoinColumn(name = "commodity_id", nullable = false)
    private Commodity commodity;

    private BigDecimal quantity;
    private String quantityUnit;
    private String packagingType;

    @ManyToOne
    @JoinColumn(name = "asset_type_id")
    private AssetType assetType;

    private String status;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;
}
