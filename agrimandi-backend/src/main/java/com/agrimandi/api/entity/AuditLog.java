package com.agrimandi.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
public class AuditLog extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "actor_id")
    private User actor;

    private String actorRole;

    @ManyToOne
    @JoinColumn(name = "market_id")
    private Market market;

    private String action;
    private String entityType;
    private UUID entityId;
    
    private String oldValue;
    private String newValue;
    
    private OffsetDateTime timestamp;
    private String ipAddress;
}
