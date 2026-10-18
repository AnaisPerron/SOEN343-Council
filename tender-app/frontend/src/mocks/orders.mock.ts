import type { Order, OrderEvent, OrderLine } from './types';

export const ordersMock: Order[] = [
  { id: 9001, customer_id: 1, restaurant_id: 1, courier_id: 4, status: 'preparing', created_at: '2026-10-18T16:10:00.000Z', accepted_at: '2026-10-18T16:12:00.000Z', rejection_reason: null },
  { id: 9002, customer_id: 2, restaurant_id: 2, courier_id: null, status: 'placed', created_at: '2026-10-18T16:18:00.000Z', accepted_at: null, rejection_reason: null },
  { id: 9003, customer_id: 1, restaurant_id: 2, courier_id: null, status: 'rejected', created_at: '2026-10-17T18:40:00.000Z', accepted_at: null, rejection_reason: 'The kitchen ran out of the selected grain.' },
  { id: 9004, customer_id: 2, restaurant_id: 1, courier_id: null, status: 'cancelled', created_at: '2026-10-16T19:05:00.000Z', accepted_at: null, rejection_reason: null },
  { id: 9005, customer_id: 1, restaurant_id: 4, courier_id: null, status: 'delivered', created_at: '2026-10-12T20:15:00.000Z', accepted_at: '2026-10-12T20:17:00.000Z', rejection_reason: null },
];

export const orderLinesMock: OrderLine[] = [
  { id: 9101, order_id: 9001, menu_item_id: 1001, quantity: 1, unit_price: 14.5 },
  { id: 9102, order_id: 9001, menu_item_id: 1002, quantity: 1, unit_price: 6.25 },
  { id: 9103, order_id: 9002, menu_item_id: 2001, quantity: 2, unit_price: 15.75 },
  { id: 9104, order_id: 9003, menu_item_id: 2002, quantity: 1, unit_price: 14 },
  { id: 9105, order_id: 9004, menu_item_id: 1001, quantity: 1, unit_price: 14.5 },
  { id: 9106, order_id: 9005, menu_item_id: 4001, quantity: 1, unit_price: 17 },
];

export const orderEventsMock: OrderEvent[] = [
  { id: 9201, order_id: 9001, previous_status: null, new_status: 'placed', event_type: 'order_placed', created_at: '2026-10-18T16:10:00.000Z' },
  { id: 9202, order_id: 9001, previous_status: 'placed', new_status: 'accepted', event_type: 'restaurant_accepted', created_at: '2026-10-18T16:12:00.000Z' },
  { id: 9203, order_id: 9001, previous_status: 'accepted', new_status: 'preparing', event_type: 'preparation_started', created_at: '2026-10-18T16:20:00.000Z' },
  { id: 9204, order_id: 9002, previous_status: null, new_status: 'placed', event_type: 'order_placed', created_at: '2026-10-18T16:18:00.000Z' },
  { id: 9205, order_id: 9003, previous_status: null, new_status: 'placed', event_type: 'order_placed', created_at: '2026-10-17T18:40:00.000Z' },
  { id: 9206, order_id: 9003, previous_status: 'placed', new_status: 'rejected', event_type: 'restaurant_rejected', created_at: '2026-10-17T18:43:00.000Z' },
  { id: 9207, order_id: 9004, previous_status: null, new_status: 'placed', event_type: 'order_placed', created_at: '2026-10-16T19:05:00.000Z' },
  { id: 9208, order_id: 9004, previous_status: 'placed', new_status: 'cancelled', event_type: 'customer_cancelled', created_at: '2026-10-16T19:07:00.000Z' },
  { id: 9209, order_id: 9005, previous_status: null, new_status: 'placed', event_type: 'order_placed', created_at: '2026-10-12T20:15:00.000Z' },
  { id: 9210, order_id: 9005, previous_status: 'placed', new_status: 'accepted', event_type: 'restaurant_accepted', created_at: '2026-10-12T20:17:00.000Z' },
  { id: 9211, order_id: 9005, previous_status: 'accepted', new_status: 'preparing', event_type: 'preparation_started', created_at: '2026-10-12T20:25:00.000Z' },
  { id: 9212, order_id: 9005, previous_status: 'preparing', new_status: 'ready', event_type: 'order_ready', created_at: '2026-10-12T20:40:00.000Z' },
  { id: 9213, order_id: 9005, previous_status: 'ready', new_status: 'picked_up', event_type: 'courier_picked_up', created_at: '2026-10-12T20:48:00.000Z' },
  { id: 9214, order_id: 9005, previous_status: 'picked_up', new_status: 'delivered', event_type: 'order_delivered', created_at: '2026-10-12T21:05:00.000Z' },
];
