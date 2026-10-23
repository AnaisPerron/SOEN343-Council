import type { Menu, MenuItem, MenuItemOption } from './types';

export const menusMock: Menu[] = [
  { id: 101, restaurant_id: 1, name: 'Maple & Main Menu' },
  { id: 102, restaurant_id: 2, name: 'Green Bowl Menu' },
  { id: 103, restaurant_id: 3, name: 'Café Nord Menu' },
  { id: 104, restaurant_id: 4, name: 'Little Italy Menu' },
];

export const menuItemsMock: MenuItem[] = [
  { id: 1001, menu_id: 101, name: 'Maple-glazed chicken sandwich', price: 14.5, status: 'available', photo_url: null },
  { id: 1002, menu_id: 101, name: 'Truffle fries', price: 6.25, status: 'available', photo_url: null },
  { id: 1003, menu_id: 101, name: 'Apple crumble', price: 7, status: 'sold_out', photo_url: null },
  { id: 2001, menu_id: 102, name: 'Harvest grain bowl', price: 15.75, status: 'available', photo_url: null },
  { id: 2002, menu_id: 102, name: 'Miso tofu bowl', price: 14, status: 'available', photo_url: null },
  { id: 3001, menu_id: 103, name: 'Smoked salmon bagel', price: 12.5, status: 'available', photo_url: null },
  { id: 4001, menu_id: 104, name: 'Margherita pizza', price: 17, status: 'available', photo_url: null },
];

export const menuItemOptionsMock: MenuItemOption[] = [
  { id: 5001, menu_item_id: 1001, name: 'Add avocado', additional_cost: 2 },
  { id: 5002, menu_item_id: 1001, name: 'Gluten-free bun', additional_cost: 1.5 },
  { id: 5003, menu_item_id: 2001, name: 'Add grilled chicken', additional_cost: 4 },
  { id: 5004, menu_item_id: 4001, name: 'Extra mozzarella', additional_cost: 2.5 },
];
