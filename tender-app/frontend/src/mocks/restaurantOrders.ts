import type { RestaurantOrder, RestaurantOrderStatus } from '../services/restaurant/orderQueue';

const customers = {
  alex: { name: 'Alex Morgan', address: '1180 Rue Saint-Denis, Montréal, QC H2X 3J5', phone: '514-555-0101' },
  sam: { name: 'Sam Chen', address: '4100 Rue Saint-Urbain, Montréal, QC H2W 1V3', phone: '514-555-0102' },
  maya: { name: 'Maya Tremblay', address: '221 Avenue du Mont-Royal E, Montréal, QC H2T 1P2', phone: '514-555-0103' },
};

function bowlOrder(id: number, status: RestaurantOrderStatus, createdAt: string, customer: keyof typeof customers): RestaurantOrder {
  return {
    id,
    restaurantId: 2,
    status,
    createdAt,
    acceptedAt: status === 'placed' || status === 'rejected' || status === 'cancelled' ? null : createdAt,
    updatedAt: createdAt,
    rejectionReason: status === 'rejected' ? 'The selected ingredient was unavailable.' : null,
    customer: customers[customer],
    lines: [
      { name: 'Harvest grain bowl', quantity: 1, unitPrice: 15.75, toppings: [{ name: 'Add grilled chicken', additionalCost: 4 }] },
      { name: 'Truffle fries', quantity: 1, unitPrice: 6.25, toppings: [] },
    ],
  };
}

export const restaurantOrdersMock: RestaurantOrder[] = [
  bowlOrder(9301, 'placed', '2026-10-23T14:05:00.000Z', 'alex'),
  bowlOrder(9302, 'placed', '2026-10-23T14:12:00.000Z', 'sam'),
  bowlOrder(9303, 'placed', '2026-10-23T14:19:00.000Z', 'maya'),
  bowlOrder(9304, 'accepted', '2026-10-23T13:54:00.000Z', 'sam'),
  bowlOrder(9305, 'preparing', '2026-10-23T13:40:00.000Z', 'maya'),
  bowlOrder(9306, 'ready', '2026-10-23T13:26:00.000Z', 'alex'),
  bowlOrder(9307, 'picked_up', '2026-10-23T12:57:00.000Z', 'sam'),
  bowlOrder(9308, 'delivered', '2026-10-23T11:12:00.000Z', 'maya'),
  bowlOrder(9309, 'rejected', '2026-10-22T18:45:00.000Z', 'alex'),
  bowlOrder(9310, 'cancelled', '2026-10-22T17:30:00.000Z', 'sam'),
  bowlOrder(9311, 'refunded', '2026-10-21T19:10:00.000Z', 'maya'),
];
