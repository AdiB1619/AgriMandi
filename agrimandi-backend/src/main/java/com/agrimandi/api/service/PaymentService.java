package com.agrimandi.api.service;

import com.agrimandi.api.dto.PaymentRequestDTO;
import com.agrimandi.api.entity.Payment;
import com.agrimandi.api.entity.Transaction;
import com.agrimandi.api.exception.BusinessValidationException;
import com.agrimandi.api.exception.ResourceNotFoundException;
import com.agrimandi.api.repository.PaymentRepository;
import com.agrimandi.api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.agrimandi.api.security.SecurityUtils;
import com.agrimandi.api.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public Payment recordPayment(UUID transactionId, PaymentRequestDTO request) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
                
        if (!"ADMIN".equals(securityUtils.getCurrentUserRole()) && !transaction.getLot().getMarket().getId().equals(securityUtils.getCurrentMarketId())) {
            throw new BusinessValidationException("You can only operate on transactions in your assigned market");
        }

        if ("CANCELLED".equals(transaction.getStatus())) {
            throw new BusinessValidationException("Cannot record payment for cancelled transaction");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("Payment amount must be greater than zero");
        }

        BigDecimal totalPaid = paymentRepository.sumAmountByTransactionId(transactionId);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        BigDecimal pendingAmount = transaction.getNetAmount().subtract(totalPaid);

        if (request.getAmount().compareTo(pendingAmount) > 0) {
            throw new BusinessValidationException("Payment amount exceeds outstanding balance. Pending amount is: " + pendingAmount);
        }

        Payment payment = new Payment();
        payment.setTransaction(transaction);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setPaymentReference(request.getPaymentReference());
        payment.setNotes(request.getNotes());
        payment.setPaymentDate(OffsetDateTime.now());
        
        userRepository.findById(securityUtils.getCurrentUserId()).ifPresent(payment::setRecordedBy);

        Payment savedPayment = paymentRepository.save(payment);

        auditService.logEvent(AuditService.PAYMENT_RECORDED, AuditService.ENTITY_PAYMENT, savedPayment.getId(),
                transaction.getLot().getMarket().getId(),
                "transaction=" + transaction.getTransactionNumber()
                        + ", amount=" + request.getAmount().toPlainString()
                        + ", method=" + request.getPaymentMethod());

        // Update transaction status
        BigDecimal newTotalPaid = totalPaid.add(request.getAmount());
        if (newTotalPaid.compareTo(transaction.getNetAmount()) >= 0) {
            transaction.setStatus("PAID");
        } else {
            transaction.setStatus("PARTIALLY_PAID");
        }
        transactionRepository.save(transaction);

        return savedPayment;
    }
    
    public BigDecimal getPendingAmount(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        BigDecimal totalPaid = paymentRepository.sumAmountByTransactionId(transactionId);
        return transaction.getNetAmount().subtract(totalPaid);
    }
}
