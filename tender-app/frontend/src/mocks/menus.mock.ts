import type { Menu, MenuItem, MenuItemOption } from './types';

export const menusMock: Menu[] = [
  { id: 101, restaurant_id: 1, name: 'Maple & Main Menu' },
  { id: 102, restaurant_id: 2, name: 'Green Bowl Menu' },
  { id: 103, restaurant_id: 3, name: 'Café Nord Menu' },
  { id: 104, restaurant_id: 4, name: 'Little Italy Menu' },
];

export const menuItemsMock: MenuItem[] = [
  { id: 1001, menu_id: 101, name: 'Maple-glazed chicken sandwich', price: 14.5, description: 'Tender chicken, maple glaze and crisp greens.', available: true, photURL: null },
  { id: 1002, menu_id: 101, name: 'Truffle fries', price: 6.25, description: 'Golden fries tossed with fragrant truffle.', available: true, photURL: null },
  { id: 1003, menu_id: 101, name: 'Apple crumble', price: 7, description: 'Warm apples under a buttery crumble.', available: false, photURL: null },
  { id: 1004, menu_id: 101, name: 'Harvest grain bowl', price: 15.75, description: 'Seasonal vegetables, grains and house dressing.', available: true, photURL: null },
  { id: 1005, menu_id: 101, name: 'Miso tofu bowl', price: 14, description: 'Miso tofu with fresh vegetables and rice.', available: true, photURL: null },
  { id: 1006, menu_id: 101, name: 'Smoked salmon bagel', price: 12.5, description: 'Smoked salmon, cream cheese and a toasted bagel.', available: true, photURL: null },
  { id: 2001, menu_id: 102, name: 'Harvest grain bowl', price: 15.75, description: 'Seasonal vegetables, grains and house dressing.', available: true, photURL: null },
  { id: 2002, menu_id: 102, name: 'Miso tofu bowl', price: 14, description: 'Miso tofu with fresh vegetables and rice.', available: true, photURL: null },
  { id: 2003, menu_id: 102, name: 'Truffle fries', price: 6.25, description: 'Golden fries tossed with fragrant truffle.', available: true, photURL: null },
  { id: 2004, menu_id: 102, name: 'Apple crumble', price: 7, description: 'Warm apples under a buttery crumble.', available: true, photURL: null },
  { id: 2005, menu_id: 102, name: 'Maple-glazed chicken sandwich', price: 14.5, description: 'Tender chicken, maple glaze and crisp greens.', available: true, photURL: null },
  { id: 3001, menu_id: 103, name: 'Smoked salmon bagel', price: 12.5, description: 'Smoked salmon, cream cheese and a toasted bagel.', available: true, photURL: null },
  { id: 3002, menu_id: 103, name: 'Maple-glazed chicken sandwich', price: 14.5, description: 'Tender chicken, maple glaze and crisp greens.', available: true, photURL: null },
  { id: 3003, menu_id: 103, name: 'Truffle fries', price: 6.25, description: 'Golden fries tossed with fragrant truffle.', available: true, photURL: null },
  { id: 3004, menu_id: 103, name: 'Apple crumble', price: 7, description: 'Warm apples under a buttery crumble.', available: true, photURL: null },
  { id: 3005, menu_id: 103, name: 'Harvest grain bowl', price: 15.75, description: 'Seasonal vegetables, grains and house dressing.', available: true, photURL: null },
  { id: 4001, menu_id: 104, name: 'Margherita pizza', price: 17, description: 'Tomato, mozzarella and fresh basil.', available: true, photURL: null },
  { id: 4002, menu_id: 104, name: 'Truffle fries', price: 6.25, description: 'Golden fries tossed with fragrant truffle.', available: true, photURL: null },
  { id: 4003, menu_id: 104, name: 'Apple crumble', price: 7, description: 'Warm apples under a buttery crumble.', available: true, photURL: null },
  { id: 4004, menu_id: 104, name: 'Harvest grain bowl', price: 15.75, description: 'Seasonal vegetables, grains and house dressing.', available: true, photURL: null },
  { id: 4005, menu_id: 104, name: 'Smoked salmon bagel', price: 12.5, description: 'Smoked salmon, cream cheese and a toasted bagel.', available: true, photURL: null },
];

// Each option belongs to one menu item and adds its cost to that item's base price.
export const menuItemOptionsMock: MenuItemOption[] = [
  { id: 5001, menu_item_id: 1001, name: 'Add avocado', additional_cost: 2 },
  { id: 5002, menu_item_id: 1001, name: 'Gluten-free bun', additional_cost: 1.5 },
  { id: 5003, menu_item_id: 2001, name: 'Add grilled chicken', additional_cost: 4 },
  { id: 5004, menu_item_id: 4001, name: 'Extra mozzarella', additional_cost: 2.5 },
  { id: 5005, menu_item_id: 4001, name: 'Add mushrooms', additional_cost: 1.75 },
  { id: 5006, menu_item_id: 1004, name: 'Add avocado', additional_cost: 2 },
  { id: 5007, menu_item_id: 2002, name: 'Extra tofu', additional_cost: 3 },
];
