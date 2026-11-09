package com.agrimandi.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "asset_types")
@Getter
@Setter
public class AssetType extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "market_id", nullable = false)
    private Market market;

    private String name;
    private String code;
    private Boolean returnable;
    private Boolean active;
}
