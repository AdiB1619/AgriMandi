package com.agrimandi.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "markets")
@Getter
@Setter
public class Market extends BaseEntity {
    private String code;
    private String name;
    private String location;
    private String status;
}
