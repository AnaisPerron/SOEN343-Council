import { requestJson } from './client.ts';

export type MenuItem = { id: number | null; name: string; description: string; category: string; price: number; available: boolean; photoUrl: string | null };

/** Shape of the backend's MenuItemDto. */
type MenuItemDto = { id: number; menu_id: number; name: string; price: number; description: string | null; available: boolean; photoUrl: string | null; category: string | null };

const BASE = '/api/restaurant/menu-items';

const fromDto = (dto: MenuItemDto): MenuItem => ({
  id: dto.id,
  name: dto.name,
  description: dto.description ?? '',
  category: dto.category ?? '',
  price: Number(dto.price),
  available: dto.available,
  photoUrl: dto.photoUrl,
});

const toRequest = (item: MenuItem) => ({
  name: item.name,
  description: item.description,
  price: item.price,
  available: item.available,
  category: item.category,
  photoUrl: item.photoUrl,
});

export const listMenuItems = async (token: string) =>
  (await requestJson<MenuItemDto[]>('GET', BASE, undefined, token)).map(fromDto);

export const createMenuItem = async (token: string, item: MenuItem) =>
  fromDto(await requestJson<MenuItemDto>('POST', BASE, toRequest(item), token));

export const updateMenuItem = async (token: string, id: number, item: MenuItem) =>
  fromDto(await requestJson<MenuItemDto>('PUT', `${BASE}/${id}`, toRequest(item), token));

export const toggleMenuItemAvailability = async (token: string, id: number) =>
  fromDto(await requestJson<MenuItemDto>('PATCH', `${BASE}/${id}/availability`, undefined, token));
