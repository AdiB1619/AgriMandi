package com.agrimandi.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "farmers")
@Getter
@Setter
public class Farmer extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "market_id", nullable = false)
    private Market market;

    private String farmerCode;
    private String name;
    private String village;
    private String phone;
    private String address;
    private String status;
}
