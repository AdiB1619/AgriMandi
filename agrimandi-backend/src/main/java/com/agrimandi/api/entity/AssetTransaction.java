package com.agrimandi.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "asset_transactions")
@Getter
@Setter
public class AssetTransaction extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne
    @JoinColumn(name = "asset_type_id", nullable = false)
    private AssetType assetType;

    private Integer quantity;
    private String movementType; // ISSUED, RETURNED, ADJUSTMENT
    private OffsetDateTime movementDate;
    private String notes;

    @ManyToOne
    @JoinColumn(name = "recorded_by")
    private User recordedBy;
}
