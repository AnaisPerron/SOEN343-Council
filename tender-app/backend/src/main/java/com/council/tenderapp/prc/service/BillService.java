package com.council.tenderapp.prc.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.council.tenderapp.ord.entities.Order;
import com.council.tenderapp.prc.entities.Bill;
import com.council.tenderapp.prc.repositories.BillRepository;

@Service 
public class BillService 
{
    private final BillRepository billRepository;

    public BillService(BillRepository billRepository) {
        this.billRepository = billRepository;
    }

    public Double calculateTotalCharge(Long orderId) 
    {
        Bill bill = billRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("No bill found for order ID: " + orderId));

        Double totalAmount = BigDecimal.valueOf(bill.getAmount())
                .add(BigDecimal.valueOf(bill.getServiceFee()))
                .add(BigDecimal.valueOf(bill.getDeliveryFee()))
                .add(BigDecimal.valueOf(bill.getSmallOrderFee())).doubleValue();
        
        return totalAmount;
    }

    public Long getBillIdByOrderId(Long orderId) 
    {
        Bill bill = billRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("No bill found for order ID: " + orderId));

        return bill.getId();
    }

    @Transactional
    public void ensureBillForRefund(Order order) {
        if (billRepository.findByOrderId(order.getId()).isPresent()) {
            return;
        }

        Bill bill = new Bill();
        bill.setOrderId(order.getId());
        bill.setCustomerId(order.getCustomerId());
        bill.setDate(OffsetDateTime.now());
        bill.setAmount(order.getTotalPrice().doubleValue());
        bill.setServiceFee(0.0);
        bill.setDeliveryFee(0.0);
        bill.setSmallOrderFee(0.0);
        billRepository.save(bill);
    }
}
