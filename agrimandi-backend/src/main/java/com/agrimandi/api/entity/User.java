package com.agrimandi.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "market_id", nullable = false)
    private Market market;

    private String username;
    private String passwordHash;
    private String role;
    private String status;

    @ManyToOne
    @JoinColumn(name = "farmer_id")
    private Farmer farmer;

    @ManyToOne
    @JoinColumn(name = "buyer_id")
    private Buyer buyer;
}
