package com.council.tenderapp.technicalservices.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.ord.exceptions.InvalidOrderException;

/** Ten whole tenders equal one CAD dollar. Entries are unique per order and reason. */
@Service
public class TenderCash {
    private final JdbcTemplate jdbc;

    public TenderCash(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Balance(long tenders, BigDecimal cadValue) {}

    @Transactional(readOnly = true)
    public Balance balance(Long customerId) {
        long tenders = balanceValue(customerId);
        return new Balance(tenders, BigDecimal.valueOf(tenders, 1));
    }

    private long balanceValue(Long customerId) {
        Long value = jdbc.queryForObject("SELECT CAST(FLOOR(COALESCE(c.td_cash_balance, 0) * 10) AS BIGINT) + "
                + "COALESCE((SELECT SUM(t.tenders) FROM tender_cash_transactions t WHERE t.customer_id = u.id), 0) "
                + "FROM users u LEFT JOIN customer_profiles c ON c.user_id = u.id WHERE u.id = ?", Long.class, customerId);
        return value == null ? 0 : value;
    }

    public void lockCustomer(Long customerId) {
        jdbc.queryForObject("SELECT id FROM users WHERE id = ? FOR UPDATE", Long.class, customerId);
    }

    public BigDecimal discount(Long customerId, long tenders, BigDecimal total) {
        if (tenders < 0 || tenders > total.movePointRight(1).setScale(0, RoundingMode.DOWN).longValueExact())
            throw new InvalidOrderException("Invalid Tender Cash amount");
        if (tenders > balanceValue(customerId)) throw new InvalidOrderException("Insufficient Tender Cash");
        return BigDecimal.valueOf(tenders, 1);
    }

    public void placed(Order order, long spent, BigDecimal paid) {
        if (spent > 0) entry(order, "SPENT", -spent);
        long earned = paid.movePointRight(1).setScale(0, RoundingMode.DOWN).longValueExact();
        if (earned > 0) entry(order, "EARNED", earned);
    }

    public void refunded(Order order) {
        Long customerId = order.getCustomerId();
        if (customerId == null) return;
        lockCustomer(customerId);
        // Remove points earned on the purchase; restore any points it spent.
        Long earned = jdbc.queryForObject("SELECT COALESCE(SUM(tenders), 0) FROM tender_cash_transactions WHERE order_id = ? AND reason = 'EARNED'", Long.class, order.getId());
        Long spent = jdbc.queryForObject("SELECT COALESCE(SUM(tenders), 0) FROM tender_cash_transactions WHERE order_id = ? AND reason = 'SPENT'", Long.class, order.getId());
        if (earned != null && earned != 0) entry(order, "EARN_REVERSED", -earned);
        if (spent != null && spent != 0) entry(order, "SPEND_RESTORED", -spent);
        BigDecimal paid = order.getTotalPrice().add(BigDecimal.valueOf(spent == null ? 0 : spent, 1));
        long credit = paid.movePointRight(1).setScale(0, RoundingMode.CEILING).longValueExact();
        if (credit > 0) entry(order, "REFUND", credit);
    }

    private void entry(Order order, String reason, long tenders) {
        jdbc.update("INSERT INTO tender_cash_transactions (customer_id, order_id, reason, tenders) VALUES (?, ?, ?, ?)",
                order.getCustomerId(), order.getId(), reason, tenders);
    }
}
