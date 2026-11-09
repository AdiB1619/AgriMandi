package com.agrimandi.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "buyers")
@Getter
@Setter
public class Buyer extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "market_id", nullable = false)
    private Market market;

    private String buyerCode;
    private String name;
    private String businessName;
    private String phone;
    private String address;
    private String licenseReference;
    private String status;
}
