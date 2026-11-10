package com.council.tenderapp.prc.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

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
}
