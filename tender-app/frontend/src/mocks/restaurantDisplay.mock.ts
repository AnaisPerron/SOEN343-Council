import type { ImageSourcePropType } from 'react-native';

// Display-only fixture fields until the restaurant API exposes these attributes.
export interface RestaurantDisplay {
  rating?: number;
  reviewCount?: number;
  address?: string;
  deliveryMinutes?: string;
  deliveryFee?: number;
  coverItemId?: number;
}

export const restaurantDisplayMock: Record<number, RestaurantDisplay> = {
  1: { rating: 4.8, reviewCount: 124, address: 'Downtown Montréal', deliveryFee: 2.99, coverItemId: 1001 },
  2: { rating: 4.6, reviewCount: 89, address: 'Montréal, QC', deliveryFee: 1.99, coverItemId: 2001 },
  3: { rating: 4.9, reviewCount: 56, address: 'Montréal, QC', deliveryFee: 2.49, coverItemId: 3001 },
  4: { rating: 4.7, reviewCount: 203, address: 'Little Italy, Montréal', deliveryFee: 3.49 },
};

// Metro requires literal local asset paths.
export const menuItemPhotos: Record<number, ImageSourcePropType> = {
  1001: require('./menu-items-imgs/maple-glazed-chicken-sandwich.jpg'),
  1002: require('./menu-items-imgs/truffle.jpg'),
  1003: require('./menu-items-imgs/apple-crumble.jpg'),
  1004: require('./menu-items-imgs/harvest-grain-bowl.jpg'),
  1005: require('./menu-items-imgs/miso-tofu-bowl.jpg'),
  1006: require('./menu-items-imgs/smoked-salmon-bagel.jpg'),
  2001: require('./menu-items-imgs/harvest-grain-bowl.jpg'),
  2002: require('./menu-items-imgs/miso-tofu-bowl.jpg'),
  2003: require('./menu-items-imgs/truffle.jpg'),
  2004: require('./menu-items-imgs/apple-crumble.jpg'),
  2005: require('./menu-items-imgs/maple-glazed-chicken-sandwich.jpg'),
  3001: require('./menu-items-imgs/smoked-salmon-bagel.jpg'),
  3002: require('./menu-items-imgs/maple-glazed-chicken-sandwich.jpg'),
  3003: require('./menu-items-imgs/truffle.jpg'),
  3004: require('./menu-items-imgs/apple-crumble.jpg'),
  3005: require('./menu-items-imgs/harvest-grain-bowl.jpg'),
  4001: require('./menu-items-imgs/margherita-pizza.jpg'),
  4002: require('./menu-items-imgs/truffle.jpg'),
  4003: require('./menu-items-imgs/apple-crumble.jpg'),
  4004: require('./menu-items-imgs/harvest-grain-bowl.jpg'),
  4005: require('./menu-items-imgs/smoked-salmon-bagel.jpg'),
};

export interface MenuItemDisplay {
  description: string;
  category: string;
}

export const menuItemDisplayMock: Record<number, MenuItemDisplay> = {
  1001: { description: 'Tender chicken, maple glaze and crisp greens.', category: 'Popular' },
  1002: { description: 'Golden fries tossed with fragrant truffle.', category: 'Sides' },
  1003: { description: 'Warm apples under a buttery crumble.', category: 'Desserts' },
  1004: { description: 'Seasonal vegetables, grains and house dressing.', category: 'Bowls' },
  1005: { description: 'Miso tofu with fresh vegetables and rice.', category: 'Bowls' },
  1006: { description: 'Smoked salmon, cream cheese and a toasted bagel.', category: 'Popular' },
  2001: { description: 'Seasonal vegetables, grains and house dressing.', category: 'Popular' },
  2002: { description: 'Miso tofu with fresh vegetables and rice.', category: 'Bowls' },
  2003: { description: 'Golden fries tossed with fragrant truffle.', category: 'Sides' },
  2004: { description: 'Warm apples under a buttery crumble.', category: 'Desserts' },
  2005: { description: 'Tender chicken, maple glaze and crisp greens.', category: 'Popular' },
  3001: { description: 'Smoked salmon, cream cheese and a toasted bagel.', category: 'Popular' },
  3002: { description: 'Tender chicken, maple glaze and crisp greens.', category: 'Popular' },
  3003: { description: 'Golden fries tossed with fragrant truffle.', category: 'Sides' },
  3004: { description: 'Warm apples under a buttery crumble.', category: 'Desserts' },
  3005: { description: 'Seasonal vegetables, grains and house dressing.', category: 'Bowls' },
  4001: { description: 'Tomato, mozzarella and fresh basil.', category: 'Pizza' },
  4002: { description: 'Golden fries tossed with fragrant truffle.', category: 'Sides' },
  4003: { description: 'Warm apples under a buttery crumble.', category: 'Desserts' },
  4004: { description: 'Seasonal vegetables, grains and house dressing.', category: 'Bowls' },
  4005: { description: 'Smoked salmon, cream cheese and a toasted bagel.', category: 'Popular' },
};
