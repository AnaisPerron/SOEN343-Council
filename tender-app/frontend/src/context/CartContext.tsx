import { createContext, useContext, useState, type ReactNode } from 'react';
import type { MenuItem } from '../mocks/types';
import { addCartLine, changeCartLineQuantity, type CartLine } from '../app/customer/cart/cartStore';

interface CartContextValue {
  lines: CartLine[];
  addItem: (item: MenuItem, restaurantId: number, optionIds: number[], optionCost: number, quantity: number) => void;
  setQuantity: (key: string, quantity: number) => void;
  clearCart: () => void;
  removeRestaurant: (restaurantId: number) => void;
}

const CartContext = createContext<CartContextValue | null>(null);

export function CartProvider({ children }: { children: ReactNode }) {
  // Keep the draft cart across customer routes; it resets when the app session reloads.
  const [lines, setLines] = useState<CartLine[]>([]);
  const addItem: CartContextValue['addItem'] = (item, restaurantId, optionIds, optionCost, quantity) => {
    // Functional updates preserve earlier cart changes if several actions happen close together.
    setLines((current) => addCartLine(current, item, restaurantId, optionIds, optionCost, quantity));
  };
  const setQuantity: CartContextValue['setQuantity'] = (key, quantity) => {
    setLines((current) => changeCartLineQuantity(current, key, quantity));
  };
  const clearCart = () => setLines([]);
  // Drop one restaurant's lines, e.g. after its order is placed while another restaurant's failed.
  const removeRestaurant = (restaurantId: number) => {
    setLines((current) => current.filter((line) => line.restaurantId !== restaurantId));
  };
  return <CartContext.Provider value={{ lines, addItem, setQuantity, clearCart, removeRestaurant }}>{children}</CartContext.Provider>;
}

export function useCart() {
  const cart = useContext(CartContext);
  if (!cart) throw new Error('useCart must be used inside CartProvider');
  return cart;
}
