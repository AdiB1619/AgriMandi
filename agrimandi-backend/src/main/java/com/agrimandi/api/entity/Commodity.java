package com.agrimandi.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "commodities")
@Getter
@Setter
public class Commodity extends BaseEntity {

    private String name;
    private String code;
    private String defaultPriceUnit;
    private Boolean activeStatus;
}
