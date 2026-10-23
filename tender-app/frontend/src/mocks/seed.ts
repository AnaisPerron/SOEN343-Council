import { menusMock, menuItemsMock, menuItemOptionsMock } from './menus.mock';
import { orderEventsMock, orderLinesMock, ordersMock } from './orders.mock';
import { restaurantsMock } from './restaurants.mock';
import { courierProfilesMock, customerProfilesMock, restaurantStaffMock, usersMock } from './users.mock';

/** Shared initial state for screens and UI tests. Reset by calling seedMockState again. */
export function seedMockState() {
  return {
    users: usersMock.map((item) => ({ ...item })),
    customerProfiles: customerProfilesMock.map((item) => ({ ...item })),
    courierProfiles: courierProfilesMock.map((item) => ({ ...item })),
    restaurantStaff: restaurantStaffMock.map((item) => ({ ...item })),
    restaurants: restaurantsMock.map((item) => ({ ...item })),
    menus: menusMock.map((item) => ({ ...item })),
    menuItems: menuItemsMock.map((item) => ({ ...item })),
    menuItemOptions: menuItemOptionsMock.map((item) => ({ ...item })),
    orders: ordersMock.map((item) => ({ ...item })),
    orderLines: orderLinesMock.map((item) => ({ ...item })),
    orderEvents: orderEventsMock.map((item) => ({ ...item })),
  };
}

/** Mutable in-memory state that can be imported by future screens or tests. */
export const mockState = seedMockState();
