import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import { fetchOpenRestaurants, fetchRestaurantMenu, type CatalogItem, type CatalogMenu } from '@/api/customerCatalog';
import type { MenuItemOption } from '@/mocks/types';
import type { Restaurant } from '@/types/restaurant';
import { useAuth } from './AuthContext';

type LoadStatus = 'loading' | 'ready' | 'error';

interface MenuEntry {
  status: LoadStatus;
  menu?: CatalogMenu;
  error?: string;
}

interface CatalogContextValue {
  restaurants: Restaurant[];
  restaurantsStatus: LoadStatus;
  restaurantsError: string | null;
  reloadRestaurants: () => void;
  menus: Record<number, MenuEntry>;
  loadMenu: (restaurantId: number) => void;
  findItem: (itemId: number) => CatalogItem | undefined;
  findOptions: (optionIds: number[]) => MenuItemOption[];
}

const CatalogContext = createContext<CatalogContextValue | null>(null);

const errorMessage = (error: unknown) => error instanceof Error ? error.message : 'Something went wrong.';

export function CatalogProvider({ children }: { children: ReactNode }) {
  const { token } = useAuth();
  const [restaurants, setRestaurants] = useState<Restaurant[]>([]);
  const [restaurantsStatus, setRestaurantsStatus] = useState<LoadStatus>('loading');
  const [restaurantsError, setRestaurantsError] = useState<string | null>(null);
  const [menus, setMenus] = useState<Record<number, MenuEntry>>({});
  // Track in-flight menu requests outside state so concurrent screens do not fetch the same menu twice.
  const pendingMenus = useRef(new Set<number>());

  const fetchRestaurants = useCallback(() => {
    if (!token) return;
    fetchOpenRestaurants(token)
      .then((loaded) => { setRestaurants(loaded); setRestaurantsStatus('ready'); })
      .catch((error) => { setRestaurantsError(errorMessage(error)); setRestaurantsStatus('error'); });
  }, [token]);

  // Status starts as 'loading', so the first fetch only updates state once the request settles.
  useEffect(fetchRestaurants, [fetchRestaurants]);

  const reloadRestaurants = () => {
    setRestaurantsStatus('loading');
    setRestaurantsError(null);
    fetchRestaurants();
  };

  const loadMenu = useCallback((restaurantId: number) => {
    if (!token || pendingMenus.current.has(restaurantId)) return;
    pendingMenus.current.add(restaurantId);
    setMenus((current) => ({ ...current, [restaurantId]: { ...current[restaurantId], status: 'loading' } }));
    fetchRestaurantMenu(token, restaurantId)
      .then((menu) => setMenus((current) => ({ ...current, [restaurantId]: { status: 'ready', menu } })))
      .catch((error) => setMenus((current) => ({ ...current, [restaurantId]: { status: 'error', error: errorMessage(error) } })))
      .finally(() => pendingMenus.current.delete(restaurantId));
  }, [token]);

  const findItem = (itemId: number) => {
    for (const entry of Object.values(menus)) {
      const item = entry.menu?.items.find((candidate) => candidate.id === itemId);
      if (item) return item;
    }
    return undefined;
  };

  const findOptions = (optionIds: number[]) => Object.values(menus)
    .flatMap((entry) => entry.menu?.options ?? [])
    .filter((option) => optionIds.includes(option.id));

  return <CatalogContext.Provider value={{ restaurants, restaurantsStatus, restaurantsError, reloadRestaurants, menus, loadMenu, findItem, findOptions }}>
    {children}
  </CatalogContext.Provider>;
}

export function useCatalog() {
  const catalog = useContext(CatalogContext);
  if (!catalog) throw new Error('useCatalog must be used inside CatalogProvider');
  return catalog;
}

/** Load a restaurant's menu on first use and return its cached entry. */
export function useRestaurantMenu(restaurantId: number | undefined) {
  const { menus, loadMenu } = useCatalog();
  const entry = restaurantId === undefined ? undefined : menus[restaurantId];
  useEffect(() => {
    if (restaurantId !== undefined && !entry) loadMenu(restaurantId);
  }, [restaurantId, entry, loadMenu]);
  return {
    menu: entry?.menu,
    // Without a restaurant there is nothing to load, so callers fall through to their not-found state.
    status: restaurantId === undefined ? 'error' : entry?.status ?? 'loading' as LoadStatus,
    error: entry?.error ?? null,
    retry: () => restaurantId !== undefined && loadMenu(restaurantId),
  };
}
