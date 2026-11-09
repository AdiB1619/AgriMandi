package com.agrimandi.api.dto.report;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class AuditLogDTO {

    private UUID id;
    private String action;
    private String entityType;
    private UUID entityId;
    private String actorUsername;
    private String actorRole;
    private UUID actorId;
    private UUID marketId;
    private String marketCode;
    private String oldValue;
    private String newValue;
    private OffsetDateTime timestamp;
}
