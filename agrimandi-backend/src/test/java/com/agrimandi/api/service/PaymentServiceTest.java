package com.agrimandi.api.service;

import com.agrimandi.api.dto.PaymentRequestDTO;
import com.agrimandi.api.entity.Payment;
import com.agrimandi.api.entity.Transaction;
import com.agrimandi.api.entity.Lot;
import com.agrimandi.api.entity.Market;
import com.agrimandi.api.exception.BusinessValidationException;
import com.agrimandi.api.repository.PaymentRepository;
import com.agrimandi.api.repository.TransactionRepository;
import com.agrimandi.api.repository.UserRepository;
import com.agrimandi.api.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private PaymentService paymentService;

    private Transaction mockTransaction;

    @BeforeEach
    void setUp() {
        Market mockMarket = new Market();
        mockMarket.setId(UUID.randomUUID());
        
        Lot mockLot = new Lot();
        mockLot.setMarket(mockMarket);

        mockTransaction = new Transaction();
        mockTransaction.setId(UUID.randomUUID());
        mockTransaction.setLot(mockLot);
        mockTransaction.setNetAmount(BigDecimal.valueOf(1000.00));
        mockTransaction.setStatus("CREATED");
    }

    @Test
    void recordPayment_shouldSucceed_andMarkPartiallyPaid() {
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setAmount(BigDecimal.valueOf(400.00));
        request.setPaymentMethod("CASH");

        when(securityUtils.getCurrentUserRole()).thenReturn("AGENT");
        when(securityUtils.getCurrentMarketId()).thenReturn(mockTransaction.getLot().getMarket().getId());

        when(transactionRepository.findById(mockTransaction.getId())).thenReturn(Optional.of(mockTransaction));
        when(paymentRepository.sumAmountByTransactionId(mockTransaction.getId())).thenReturn(BigDecimal.valueOf(100.00)); // Already paid 100
        
        Payment savedPayment = new Payment();
        savedPayment.setAmount(BigDecimal.valueOf(400.00));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        Payment response = paymentService.recordPayment(mockTransaction.getId(), request);

        assertNotNull(response);
        assertEquals("PARTIALLY_PAID", mockTransaction.getStatus());
        verify(transactionRepository).save(mockTransaction);
    }

    @Test
    void recordPayment_shouldSucceed_andMarkPaid() {
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setAmount(BigDecimal.valueOf(900.00));
        request.setPaymentMethod("BANK_TRANSFER");

        when(securityUtils.getCurrentUserRole()).thenReturn("AGENT");
        when(securityUtils.getCurrentMarketId()).thenReturn(mockTransaction.getLot().getMarket().getId());

        when(transactionRepository.findById(mockTransaction.getId())).thenReturn(Optional.of(mockTransaction));
        when(paymentRepository.sumAmountByTransactionId(mockTransaction.getId())).thenReturn(BigDecimal.valueOf(100.00));
        
        Payment savedPayment = new Payment();
        savedPayment.setAmount(BigDecimal.valueOf(900.00));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        Payment response = paymentService.recordPayment(mockTransaction.getId(), request);

        assertNotNull(response);
        assertEquals("PAID", mockTransaction.getStatus());
        verify(transactionRepository).save(mockTransaction);
    }

    @Test
    void recordPayment_exceedsPendingAmount_shouldThrowException() {
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setAmount(BigDecimal.valueOf(1000.00)); // Total would be 1100

        when(securityUtils.getCurrentUserRole()).thenReturn("AGENT");
        when(securityUtils.getCurrentMarketId()).thenReturn(mockTransaction.getLot().getMarket().getId());

        when(transactionRepository.findById(mockTransaction.getId())).thenReturn(Optional.of(mockTransaction));
        when(paymentRepository.sumAmountByTransactionId(mockTransaction.getId())).thenReturn(BigDecimal.valueOf(100.00));

        assertThrows(BusinessValidationException.class, () -> paymentService.recordPayment(mockTransaction.getId(), request));
        verify(paymentRepository, never()).save(any());
    }
}
