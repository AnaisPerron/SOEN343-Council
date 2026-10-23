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
  // TODO: Replace these zero delivery fees once delivery pricing is supported.
  1: { rating: 4.8, reviewCount: 124, address: '1200 Rue Sainte-Catherine O, Montréal, QC', deliveryFee: 0, coverItemId: 1001 },
  2: { rating: 4.6, reviewCount: 89, address: '4000 Boulevard Saint-Laurent, Montréal, QC', deliveryFee: 0, coverItemId: 2001 },
  3: { rating: 4.9, reviewCount: 56, address: '1450 Boulevard De Maisonneuve O, Montréal, QC', deliveryFee: 0, coverItemId: 3001 },
  4: { rating: 4.7, reviewCount: 203, address: '6500 Boulevard Saint-Laurent, Montréal, QC', deliveryFee: 0 },
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
  category: string;
}

export const menuItemDisplayMock: Record<number, MenuItemDisplay> = {
  1001: { category: 'Popular' },
  1002: { category: 'Sides' },
  1003: { category: 'Desserts' },
  1004: { category: 'Bowls' },
  1005: { category: 'Bowls' },
  1006: { category: 'Popular' },
  2001: { category: 'Popular' },
  2002: { category: 'Bowls' },
  2003: { category: 'Sides' },
  2004: { category: 'Desserts' },
  2005: { category: 'Popular' },
  3001: { category: 'Popular' },
  3002: { category: 'Popular' },
  3003: { category: 'Sides' },
  3004: { category: 'Desserts' },
  3005: { category: 'Bowls' },
  4001: { category: 'Pizza' },
  4002: { category: 'Sides' },
  4003: { category: 'Desserts' },
  4004: { category: 'Bowls' },
  4005: { category: 'Popular' },
};
