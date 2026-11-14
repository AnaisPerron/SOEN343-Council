package com.council.tenderapp.prc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.council.tenderapp.prc.entities.LedgerEntry;
import com.council.tenderapp.prc.repositories.LedgerEntryRepository;

class LedgerServiceTest {
    @Test
    void createsThenUpdatesPaymentRecordAndKeepsItOnRefund() {
        LedgerEntryRepository repository = mock(LedgerEntryRepository.class);
        BillService bills = mock(BillService.class);
        when(bills.calculateTotalCharge(8L)).thenReturn(14.10);
        when(bills.getBillIdByOrderId(8L)).thenReturn(3L);
        when(repository.findByOrderId(8L)).thenReturn(Optional.empty());
        when(repository.save(any(LedgerEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        LedgerService service = new LedgerService(repository, bills);

        LedgerEntry payment = service.createLedgerEntry(8L, "PLACED");
        assertThat(payment.getTransactionType()).isEqualTo("Payment");
        assertThat(payment.getFinalOrderStatus()).isEqualTo("placed");
        assertThat(payment.getTotalCharge()).isEqualTo(14.10);

        when(repository.findByOrderId(8L)).thenReturn(Optional.of(payment));
        assertThat(service.createLedgerEntry(8L, "DELIVERED")).isSameAs(payment);
        assertThat(payment.getFinalOrderStatus()).isEqualTo("delivered");
        LedgerEntry refund = service.createLedgerEntry(8L, "REFUNDED");
        assertThat(refund).isNotSameAs(payment);
        assertThat(refund.getTransactionType()).isEqualTo("Refund");
        assertThat(refund.getTotalCharge()).isEqualTo(14.10);
        assertThat(payment.getTransactionType()).isEqualTo("Payment");
        assertThat(payment.getFinalOrderStatus()).isEqualTo("delivered");
        assertThat(refund.getFinalOrderStatus()).isEqualTo("refunded");
        ArgumentCaptor<LedgerEntry> saved = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(repository, org.mockito.Mockito.times(3)).save(saved.capture());
        assertThat(saved.getAllValues()).contains(payment, refund);
    }
}
