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
@Table(name = "payments")
@Getter
@Setter
public class Payment extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    private BigDecimal amount;
    private String paymentMethod;
    private String paymentReference;
    private OffsetDateTime paymentDate;
    private String notes;

    @ManyToOne
    @JoinColumn(name = "recorded_by")
    private User recordedBy;
}
