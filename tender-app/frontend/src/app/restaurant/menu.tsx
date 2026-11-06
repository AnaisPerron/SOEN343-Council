import { useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Image, Pressable, StyleSheet, Text, TextInput, useWindowDimensions, View } from 'react-native';
import { Redirect, useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { DetailPage } from '../customer/components/DetailPage';
import { styles as customerStyles } from '../customer/restaurant/restaurant.styles';
import { useAuth } from '@/context/AuthContext';
import { createMenuItem, listMenuItems, toggleMenuItemAvailability, updateMenuItem, uploadMenuPhoto, type MenuItem } from '@/api/restaurantMenu';
import { resolvePhotoUrl } from '@/api/customerCatalog';
import { CoverImagePicker } from '@/components/restaurant/CoverImagePicker';
import { readSessionToken } from '@/context/sessionStorage';
import { readRestaurantCover, writeRestaurantCover } from '@/storage/restaurantCover';

const newItem = (): MenuItem => ({ id: null, name: '', description: '', category: '', price: 0, available: true, photoUrl: null });
const money = (value: number) => Number.isFinite(value) ? value.toFixed(2) : '0.00';
const sameItem = (a: MenuItem, b: MenuItem) => a.name === b.name && a.description === b.description && a.category === b.category
  && a.price === b.price && a.available === b.available && a.photoUrl === b.photoUrl;

export default function EditMenu() {
  const router = useRouter();
  const { user, ready } = useAuth();
  const insets = useSafeAreaInsets();
  const { height } = useWindowDimensions();
  const [items, setItems] = useState<MenuItem[] | null>(null);
  const [cover, setCover] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');
  const [expanded, setExpanded] = useState<number | null>(null);
  const [token, setToken] = useState<string | null>(null);
  // Last version of each item the backend confirmed, used to skip unchanged items on save
  const savedItems = useRef(new Map<number, MenuItem>());

  useEffect(() => {
    if (!ready || user?.role !== 'restaurant') return;
    let active = true;
    readSessionToken().then((sessionToken) => {
      if (active) setToken(sessionToken);
      if (!sessionToken) throw new Error('Please sign in again');
      return listMenuItems(sessionToken);
    }).then((loaded) => {
      if (!active) return;
      savedItems.current = new Map(loaded.map((item) => [item.id!, item]));
      setCover(readRestaurantCover(user.id) ?? null);
      setItems(loaded);
    }).catch((error) => { if (active) setMessage(error instanceof Error ? error.message : 'Could not load menu.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [ready, user?.role, user?.id]);

  const updateItem = (index: number, changes: Partial<MenuItem>) => setItems((current) => current && current.map((item, position) => position === index ? { ...item, ...changes } : item));
  const selectCover = (dataUri: string) => {
    if (!user) return;
    setMessage('');
    try { writeRestaurantCover(user.id, dataUri); }
    catch { setMessage('Image selected for this session, but browser storage is full.'); }
    setCover(dataUri);
  };
  const removeCover = () => {
    if (!user) return;
    try { writeRestaurantCover(user.id, null); }
    catch { setMessage('Cover removed for this session, but browser storage is full.'); }
    setCover(null);
  };
  // Picked photos stay as data URIs until save, when they are uploaded to the backend
  const selectItemPhoto = (index: number, dataUri: string) => { setMessage(''); updateItem(index, { photoUrl: dataUri }); };
  const changeItemPhotoUrl = (index: number, photoUrl: string) => updateItem(index, { photoUrl: photoUrl.trim() || null });
  const removeItemPhoto = (index: number) => updateItem(index, { photoUrl: null });
  const setAvailability = async (index: number, available: boolean) => {
    const item = items?.[index];
    if (!item || item.available === available) return;
    // New items send availability with their first save; existing ones flip right away
    if (item.id == null || !token) { updateItem(index, { available }); return; }
    setMessage('');
    try {
      const toggled = await toggleMenuItemAvailability(token, item.id);
      const saved = savedItems.current.get(item.id);
      if (saved) savedItems.current.set(item.id, { ...saved, available: toggled.available });
      updateItem(index, { available: toggled.available });
    } catch (error) { setMessage(error instanceof Error ? error.message : 'Could not update availability.'); }
  };
  const save = async () => {
    if (!items || !token || saving) return;
    if (items.some((item) => !item.name.trim() || !item.category.trim() || item.price < 0)) {
      setMessage('Complete each name, category, and nonnegative price before saving.'); return;
    }
    setSaving(true); setMessage('');
    const next = [...items];
    try {
      for (const [index, item] of items.entries()) {
        if (item.photoUrl?.startsWith('data:')) {
          // Keep the uploaded path in place so a failed item save doesn't upload the photo again
          next[index] = { ...item, photoUrl: await uploadMenuPhoto(token, item.photoUrl) };
        }
        const outgoing = next[index];
        const previous = outgoing.id == null ? undefined : savedItems.current.get(outgoing.id);
        if (previous && sameItem(previous, outgoing)) continue;
        const saved = outgoing.id == null ? await createMenuItem(token, outgoing) : await updateMenuItem(token, outgoing.id, outgoing);
        savedItems.current.set(saved.id!, saved);
        next[index] = saved;
      }
      setMessage('Menu saved.');
    }
    catch (error) { setMessage(error instanceof Error ? error.message : 'Could not save menu.'); }
    finally {
      // Keep the ids of items that did save so a retry doesn't create them twice
      setItems(next);
      setSaving(false);
    }
  };

  if (!ready) return <View style={local.center}><ActivityIndicator color="#D42F50" /></View>;
  if (!user) return <Redirect href="/auth/login" />;
  if (user.role !== 'restaurant') return <Redirect href="/account" />;
  if (loading) return <View style={local.center}><ActivityIndicator color="#D42F50" /></View>;
  if (!items) return <View style={local.center}>
    <Text style={local.emptyTitle}>We couldn&apos;t fetch a menu for your restaurant.</Text>
    <Text style={local.emptyDetail}>{message}</Text>
    <Pressable accessibilityRole="button" onPress={() => router.back()}><Text style={local.link}>Back</Text></Pressable>
  </View>;
  const coverHeight = Math.max(290, Math.min(height * 0.39, 390));
  const categories = [...new Set(items.map((item) => item.category.trim() || 'Menu'))];
  const editing = expanded == null ? undefined : items[expanded];
  return <DetailPage title={user.name} cover={cover ? { uri: cover } : undefined} coverHeight={coverHeight} foreground={<><View style={[local.topBar, { top: insets.top + 12 }]}>
    <Pressable style={local.topButton} accessibilityRole="button" onPress={() => router.back()}><Text style={local.topButtonText}>‹</Text></Pressable>
    <Pressable style={local.saveButton} accessibilityRole="button" accessibilityState={{ disabled: saving }} disabled={saving} onPress={save}><Text style={local.saveText}>{saving ? 'Saving…' : 'Save menu'}</Text></Pressable>
  </View><View style={{ position: 'absolute', top: coverHeight - 58, right: 18 }}><CoverImagePicker compact onSelect={selectCover} onError={setMessage} /></View></>}>
    <View style={customerStyles.info}>
      <Text style={local.coverNote}>Cover images stay on this device.</Text>
      {!!cover && <Pressable accessibilityRole="button" onPress={removeCover}><Text style={local.link}>Remove cover image</Text></Pressable>}
    </View>
    <View style={customerStyles.menu}>
      <Text style={customerStyles.menuHeading}>Menu</Text>
      {items.length === 0 && <Text style={local.emptyItems}>Your menu is empty. Add an item to get started.</Text>}
      {categories.map((category) => <View key={category} style={customerStyles.categorySection}>
        <Text style={customerStyles.categoryHeading}>{category}</Text>
        <View style={customerStyles.itemGrid}>{items.map((item, index) => (item.category.trim() || 'Menu') === category && <Pressable key={item.id ?? `new-${index}`} style={customerStyles.menuCard} accessibilityRole="button" onPress={() => setExpanded(expanded === index ? null : index)}>
          <View style={customerStyles.itemImageWrap}>{item.photoUrl ? <Image source={{ uri: resolvePhotoUrl(item.photoUrl)! }} style={customerStyles.itemImage} /> : <View style={customerStyles.itemImageFallback}><Text style={customerStyles.itemImageFallbackText}>Tender</Text></View>}</View>
          <View style={customerStyles.itemTitleRow}><Text style={customerStyles.itemName}>{item.name || 'New item'}</Text></View>
          {!!item.description && <Text style={customerStyles.itemDescription} numberOfLines={2}>{item.description}</Text>}
          {!item.available && <Text style={customerStyles.itemAvailability}>Sold out</Text>}
          <Text style={customerStyles.itemPrice}>${money(item.price)}</Text>
          <Text style={local.editHint}>{expanded === index ? 'Close editor' : 'Edit item'}</Text>
        </Pressable>)}</View>
      </View>)}
      {expanded != null && editing && <View style={local.editor}>
        <Text style={customerStyles.categoryHeading}>Edit item</Text>
        <Field label="Item name" value={editing.name} placeholder="e.g. Margherita pizza" onChangeText={(name) => updateItem(expanded, { name })} />
        <Field label="Description" value={editing.description} placeholder="Describe this item" onChangeText={(description) => updateItem(expanded, { description })} multiline />
        <Field label="Category" value={editing.category} placeholder="e.g. Pizza" onChangeText={(category) => updateItem(expanded, { category })} />
        <Field label="Price ($)" value={editing.price ? String(editing.price) : ''} placeholder="0.00" onChangeText={(value) => updateItem(expanded, { price: Number(value) || 0 })} keyboardType="decimal-pad" />
        <CoverImagePicker label="Menu item photo" onSelect={(dataUri) => selectItemPhoto(expanded, dataUri)} onError={setMessage} />
        {!!editing.photoUrl && <Pressable accessibilityRole="button" onPress={() => removeItemPhoto(expanded)}><Text style={local.link}>Remove item photo</Text></Pressable>}
        <Field label="Photo URL" value={editing.photoUrl?.startsWith('data:') ? '' : editing.photoUrl ?? ''} placeholder="https://example.com/pizza.jpg" onChangeText={(photoUrl) => changeItemPhotoUrl(expanded, photoUrl)} />
        <Text style={local.label}>Availability</Text><View style={local.chips}><Chip label="Available" selected={editing.available} onPress={() => setAvailability(expanded, true)} /><Chip label="Sold out" selected={!editing.available} onPress={() => setAvailability(expanded, false)} /></View>
      </View>}
      <Pressable style={local.addButton} accessibilityRole="button" onPress={() => { setItems([...items, newItem()]); setExpanded(items.length); }}><Text style={local.addText}>+ Add menu item</Text></Pressable>
      {!!message && <Text style={local.message}>{message}</Text>}
      <Pressable style={local.bottomSave} accessibilityRole="button" disabled={saving} onPress={save}><Text style={local.saveText}>{saving ? 'Saving…' : 'Save menu'}</Text></Pressable>
    </View>
  </DetailPage>;
}

function Field({ label, large, ...props }: { label: string; large?: boolean } & React.ComponentProps<typeof TextInput>) {
  return <View style={local.field}><Text style={local.label}>{label}</Text><TextInput {...props} style={[local.input, large && local.largeInput]} placeholderTextColor="#9F9092" /></View>;
}
function Chip({ label, selected, onPress }: { label: string; selected: boolean; onPress: () => void }) {
  return <Pressable accessibilityRole="button" accessibilityState={{ selected }} onPress={onPress} style={[local.chip, selected && local.selectedChip]}><Text style={[local.chipText, selected && local.selectedChipText]}>{label}</Text></Pressable>;
}
const local = StyleSheet.create({
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 14, paddingHorizontal: 24, backgroundColor: '#FFF' }, emptyTitle: { color: '#302629', fontSize: 22, fontWeight: '800', textAlign: 'center' }, emptyDetail: { color: '#887B7E', textAlign: 'center' }, emptyItems: { color: '#887B7E', marginTop: 20 }, coverNote: { color: '#887B7E', fontSize: 12, marginTop: 8 }, topBar: { position: 'absolute', left: 18, right: 18, flexDirection: 'row', justifyContent: 'space-between' }, topButton: { width: 42, height: 42, borderRadius: 21, backgroundColor: '#302629', alignItems: 'center', justifyContent: 'center' }, topButtonText: { color: '#FFF', fontSize: 30 }, saveButton: { backgroundColor: '#D42F50', paddingHorizontal: 18, justifyContent: 'center', borderRadius: 21 }, saveText: { color: '#FFF', fontWeight: '800' }, field: { marginTop: 15 }, label: { color: '#887B7E', fontSize: 11, fontWeight: '700', marginBottom: 6 }, input: { borderWidth: 1, borderColor: '#EDE6E6', borderRadius: 10, paddingHorizontal: 12, paddingVertical: 9, color: '#302629', fontSize: 14, backgroundColor: '#FFF' }, largeInput: { fontFamily: 'LoveLight', fontSize: 38, fontWeight: '400' }, chips: { flexDirection: 'row', gap: 8, marginTop: 8 }, chip: { borderRadius: 16, backgroundColor: '#F7F3F3', paddingHorizontal: 13, paddingVertical: 8 }, selectedChip: { backgroundColor: '#D42F50' }, chipText: { color: '#887B7E', fontSize: 12, fontWeight: '700', textTransform: 'capitalize' }, selectedChipText: { color: '#FFF' }, editHint: { color: '#D42F50', fontSize: 11, fontWeight: '700', marginTop: 7 }, editor: { backgroundColor: '#FFF8F9', borderRadius: 18, padding: 18, marginVertical: 20 }, link: { color: '#D42F50', fontWeight: '800', marginTop: 15 }, addButton: { borderWidth: 1, borderColor: '#D42F50', borderRadius: 14, alignItems: 'center', padding: 14, marginTop: 15 }, addText: { color: '#D42F50', fontWeight: '800' }, message: { color: '#302629', marginTop: 16 }, bottomSave: { backgroundColor: '#D42F50', borderRadius: 14, alignItems: 'center', padding: 16, marginVertical: 20 },
});
