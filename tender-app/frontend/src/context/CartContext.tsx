import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import type { MenuItem } from '../mocks/types';
import { addCartLine, changeCartLineQuantity, type CartLine } from '../app/customer/cart/cartStore';
import { loadCustomerCart, saveCustomerCart } from '../storage/customerCart';

interface CartContextValue {
  lines: CartLine[];
  addItem: (item: MenuItem, restaurantId: number, optionIds: number[], optionCost: number, quantity: number) => void;
  setQuantity: (key: string, quantity: number) => void;
  clearCart: () => void;
  removeRestaurant: (restaurantId: number) => void;
}

const CartContext = createContext<CartContextValue | null>(null);

export function CartProvider({ userId, children }: { userId?: number; children: ReactNode }) {
  // Keep the draft cart across customer routes and restore it after a page reload.
  const [lines, setLines] = useState<CartLine[]>(() => loadCustomerCart(userId));
  // Save after every add, remove or quantity change, including when the cart becomes empty.
  useEffect(() => saveCustomerCart(userId, lines), [userId, lines]);
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
