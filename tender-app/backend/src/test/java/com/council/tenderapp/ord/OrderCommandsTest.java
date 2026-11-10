package com.council.tenderapp.ord;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Test;

import com.council.tenderapp.ord.commands.OrderCommandInvoker;
import com.council.tenderapp.ord.commands.OrderCommands;
import com.council.tenderapp.ord.services.OrderService;

class OrderCommandsTest {

    private final OrderService orderService = mock(OrderService.class);
    private final OrderCommandInvoker invoker = new OrderCommandInvoker(orderService);

    @Test
    void commandsDelegateEveryRestaurantOrderActionToTheExistingReceiver() {
        invoker.execute(new OrderCommands.Accept(1L, 10L));
        invoker.execute(new OrderCommands.Reject(1L, 11L, "Out of stock"));
        invoker.execute(new OrderCommands.Cancel(1L, 12L));
        invoker.execute(new OrderCommands.StartPreparation(1L, 13L));
        invoker.execute(new OrderCommands.MarkReady(1L, 14L));
        invoker.execute(new OrderCommands.Refund(1L, 17L));

        verify(orderService).acceptOrder(1L, 10L);
        verify(orderService).rejectOrder(1L, 11L, "Out of stock");
        verify(orderService).cancelOrder(1L, 12L);
        verify(orderService).startPreparation(1L, 13L);
        verify(orderService).markReady(1L, 14L);
        verify(orderService).refundOrder(1L, 17L);
        verifyNoMoreInteractions(orderService);
    }
}