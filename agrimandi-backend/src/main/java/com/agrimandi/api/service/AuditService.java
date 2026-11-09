package com.agrimandi.api.service;

import com.agrimandi.api.entity.AuditLog;
import com.agrimandi.api.entity.Market;
import com.agrimandi.api.entity.User;
import com.agrimandi.api.repository.AuditLogRepository;
import com.agrimandi.api.repository.MarketRepository;
import com.agrimandi.api.repository.UserRepository;
import com.agrimandi.api.security.CustomUserDetails;
import com.agrimandi.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Single write-side entry point for the audit trail.
 *
 * <p>Audit entries are always written inside the caller's transaction so that the business
 * record and its audit event commit or roll back together. Callers must therefore be
 * {@code @Transactional}.
 *
 * <p>Authentication credentials and other security internals are never recorded here;
 * only business-level metadata (amounts, quantities, statuses) is captured.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    public static final String LOT_CREATED = "LOT_CREATED";
    public static final String WEIGHMENT_RECORDED = "WEIGHMENT_RECORDED";
    public static final String AUCTION_STARTED = "AUCTION_STARTED";
    public static final String BID_RECORDED = "BID_RECORDED";
    public static final String AUCTION_CLOSED = "AUCTION_CLOSED";
    public static final String WINNER_CONFIRMED = "WINNER_CONFIRMED";
    public static final String TRANSACTION_CREATED = "TRANSACTION_CREATED";
    public static final String PAYMENT_RECORDED = "PAYMENT_RECORDED";
    public static final String ASSET_ISSUED = "ASSET_ISSUED";
    public static final String ASSET_RETURNED = "ASSET_RETURNED";

    public static final String ENTITY_LOT = "LOT";
    public static final String ENTITY_AUCTION = "AUCTION";
    public static final String ENTITY_BID = "BID";
    public static final String ENTITY_TRANSACTION = "TRANSACTION";
    public static final String ENTITY_PAYMENT = "PAYMENT";
    public static final String ENTITY_ASSET_TRANSACTION = "ASSET_TRANSACTION";

    private final AuditLogRepository auditLogRepository;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final MarketRepository marketRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void logEvent(String action, String entityType, UUID entityId) {
        write(action, entityType, entityId, null, null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void logEvent(String action, String entityType, UUID entityId, UUID marketId) {
        write(action, entityType, entityId, marketId, null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void logEvent(String action, String entityType, UUID entityId, UUID marketId, String details) {
        write(action, entityType, entityId, marketId, details);
    }

    private void write(String action, String entityType, UUID entityId, UUID marketId, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setTimestamp(OffsetDateTime.now());
        if (details != null) {
            auditLog.setNewValue(details);
        }

        CustomUserDetails principal = securityUtils.getCurrentUserDetails();
        if (principal != null) {
            auditLog.setActorRole(principal.getAuthorities().stream()
                    .findFirst()
                    .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                    .orElse(null));
        }

        UUID currentUserId = securityUtils.getCurrentUserId();
        if (currentUserId != null) {
            userRepository.findById(currentUserId).ifPresent(auditLog::setActor);
        }

        UUID resolvedMarketId = marketId != null ? marketId : securityUtils.getCurrentMarketId();
        if (resolvedMarketId != null) {
            Market market = marketRepository.findById(resolvedMarketId).orElse(null);
            auditLog.setMarket(market);
        }

        auditLogRepository.save(auditLog);
    }
}
