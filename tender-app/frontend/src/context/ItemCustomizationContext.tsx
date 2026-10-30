import { createContext, useContext, useState, type ReactNode } from 'react';

interface ItemCustomizationContextValue {
  selections: Record<number, number[]>;
  saveSelection: (itemId: number, optionIds: number[]) => void;
}

const ItemCustomizationContext = createContext<ItemCustomizationContextValue | null>(null);

export function ItemCustomizationProvider({ children }: { children: ReactNode }) {
  // Save only after the customer presses Save on the Customize page.
  const [selections, setSelections] = useState<Record<number, number[]>>({});
  const saveSelection = (itemId: number, optionIds: number[]) => {
    setSelections((current) => ({ ...current, [itemId]: [...optionIds] }));
  };
  return <ItemCustomizationContext.Provider value={{ selections, saveSelection }}>{children}</ItemCustomizationContext.Provider>;
}

export function useItemCustomization() {
  const customization = useContext(ItemCustomizationContext);
  if (!customization) throw new Error('useItemCustomization must be used inside ItemCustomizationProvider');
  return customization;
}
