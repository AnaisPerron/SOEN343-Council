package com.council.tenderapp.technicalservices.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.entities.OrderStatus;
import com.council.tenderapp.ord.exceptions.InvalidOrderException;
import static org.mockito.Mockito.mock;

class TenderCashTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final TenderCash cash = new TenderCash(jdbc);

    @Test
    void redemptionUsesTenTendersPerDollarAndCannotExceedBalanceOrPrice() {
        when(jdbc.queryForObject(contains("SUM(t.tenders)"), eq(Long.class), eq(7L))).thenReturn(120L);
        assertThat(cash.discount(7L, 100, new BigDecimal("12.34"))).isEqualByComparingTo("10.0");
        assertThatThrownBy(() -> cash.discount(7L, 121, new BigDecimal("12.34")))
                .isInstanceOf(InvalidOrderException.class);
        assertThatThrownBy(() -> cash.discount(7L, 124, new BigDecimal("12.34")))
                .isInstanceOf(InvalidOrderException.class);
    }

    @Test
    void refundRestoresSpentTendersAndRoundsPaidCashUp() {
        Order order = new Order(7L, 3L, OrderStatus.DELIVERED, new BigDecimal("12.34"));
        order.setId(8L);
        when(jdbc.queryForObject(contains("reason = 'EARNED'"), eq(Long.class), eq(8L))).thenReturn(100L);
        when(jdbc.queryForObject(contains("reason = 'SPENT'"), eq(Long.class), eq(8L))).thenReturn(-20L);
        cash.refunded(order);
        verify(jdbc).update(contains("tender_cash_transactions"), eq(7L), eq(8L), eq("EARN_REVERSED"), eq(-100L));
        verify(jdbc).update(contains("tender_cash_transactions"), eq(7L), eq(8L), eq("SPEND_RESTORED"), eq(20L));
        verify(jdbc).update(contains("tender_cash_transactions"), eq(7L), eq(8L), eq("REFUND"), eq(104L));
    }

    @Test
    void purchaseEarnsTenTendersPerWholeDollarPaid() {
        Order order = new Order(7L, 3L, OrderStatus.PLACED, new BigDecimal("12.54"));
        order.setId(9L);
        cash.placed(order, 2, new BigDecimal("12.34"));
        verify(jdbc).update(contains("tender_cash_transactions"), eq(7L), eq(9L), eq("SPENT"), eq(-2L));
        verify(jdbc).update(contains("tender_cash_transactions"), eq(7L), eq(9L), eq("EARNED"), eq(123L));
    }
}
