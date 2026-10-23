//I, Anais Perron, certify that Codex helped me in generating the Template of this display. However, it has been heavily modified by myself
import { useMemo, useRef, useState } from 'react';
import {
  Image,
  ScrollView,
  Text,
  TextInput,
  TouchableOpacity,
  useWindowDimensions,
  View,
  type ImageSourcePropType,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { restaurantsMock } from '../../../mocks/restaurants.mock';
import { menusMock, menuItemsMock } from '../../../mocks/menus.mock';
import { menuItemDisplayMock, menuItemPhotos, restaurantDisplayMock } from '../../../mocks/restaurantDisplay.mock';
import type { MenuItem } from '../../../mocks/types';
import type { Restaurant } from '../../../types/restaurant';
import { styles } from './restaurant.styles';

const PANEL_OVERLAP = 30;
const STICKY_HEIGHT = 106;

function IconButton({ label, symbol, onPress, light = false }: {
  label: string;
  symbol: string;
  onPress: () => void;
  light?: boolean;
}) {
  return (
    <TouchableOpacity
      accessibilityRole="button"
      accessibilityLabel={label}
      onPress={onPress}
      style={[styles.iconButton, light && styles.lightIconButton]}
    >
      <Text style={[styles.iconText, light && styles.lightIconText]}>{symbol}</Text>
    </TouchableOpacity>
  );
}

function RestaurantCover({ restaurant, source, height }: {
  restaurant: Restaurant;
  source?: ImageSourcePropType;
  height: number;
}) {
  return (
    <View pointerEvents="none" style={[styles.cover, { height }]}>
      {source ? <Image source={source} style={styles.coverImage} resizeMode="cover" accessibilityLabel={`${restaurant.name} cover`} /> : (
        <View style={styles.coverFallback}><Text style={styles.coverFallbackText}>{restaurant.name}</Text></View>
      )}
    </View>
  );
}

function RestaurantInfoPanel({ restaurant, showMore }: {
  restaurant: Restaurant;

  
  showMore: boolean;
}) {
  const details = restaurantDisplayMock[restaurant.id];
  return (
    <View style={styles.info}>
      <Text style={styles.restaurantName}>{restaurant.name}</Text>
      <View style={styles.ratingLine}>
        {details?.rating != null && <Text style={styles.rating}>★ {details.rating.toFixed(1)}</Text>}
        {details?.reviewCount != null && <Text style={styles.muted}>({details.reviewCount} reviews)</Text>}
        <Text style={styles.dot}>·</Text>
        <Text style={styles.muted}>{restaurant.status === 'open' ? 'Open now' : restaurant.status === 'paused' ? 'Temporarily paused' : 'Closed now'}</Text>
      </View>
      {details?.address && <Text style={styles.address}>{details.address}</Text>}
      <View style={styles.fulfillmentRow}>
        <View style={styles.fulfillmentBlock}>
          <Text style={styles.fulfillmentValue}>{details?.deliveryFee != null ? `$${details.deliveryFee.toFixed(2)}` : 'See checkout'}</Text>
          <Text style={styles.fulfillmentLabel}>Delivery fee</Text>
        </View>
      </View>
      {showMore && <Text style={styles.moreDetails}>{details?.address ?? 'Address unavailable'} · {restaurant.status === 'open' ? 'Accepting orders' : 'Ordering unavailable right now'}</Text>}
    </View>
  );
}

function MenuItemCard({ item, onAdd }: { item: MenuItem; onAdd: (item: MenuItem) => void }) {
  const photo = item.photURL ? { uri: item.photURL } : menuItemPhotos[item.id];
  return (
    <View style={styles.menuCard}>
      <View style={styles.itemImageWrap}>
        {photo ? <Image source={photo} style={styles.itemImage} resizeMode="cover" accessibilityLabel={item.name} /> : <View style={styles.itemImageFallback}><Text style={styles.itemImageFallbackText}>Tender</Text></View>}
      </View>
      <View style={styles.itemTitleRow}>
        <Text style={styles.itemName}>{item.name}</Text>
        <TouchableOpacity style={[styles.addButton, !item.available && styles.addButtonDisabled]} accessibilityRole="button" accessibilityLabel={`Add ${item.name}`} accessibilityState={{ disabled: !item.available }} disabled={!item.available} onPress={() => onAdd(item)}>
          <Text style={styles.addButtonText}>+</Text>
        </TouchableOpacity>
      </View>
      {item.description && <Text style={styles.itemDescription} numberOfLines={2}>{item.description}</Text>}
      <Text style={styles.itemPrice}>{!item.available ? 'Sold out · ' : ''}${item.price.toFixed(2)}</Text>
    </View>
  );
}

export default function RestaurantDetails() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { height: windowHeight } = useWindowDimensions();
  const scrollRef = useRef<ScrollView>(null);
  const sectionOffsets = useRef<Record<string, number>>({});
  const [infoHeight, setInfoHeight] = useState(0);
  const [stickyVisible, setStickyVisible] = useState(false);
  const [coverControlsVisible, setCoverControlsVisible] = useState(true);
  const [favorite, setFavorite] = useState(false);
  const [moreOpen, setMoreOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [query, setQuery] = useState('');
  const restaurant = restaurantsMock.find((entry) => entry.id === Number(id));
  const goToRestaurants = () => router.replace('/customer/home');
  const coverHeight = Math.max(290, Math.min(windowHeight * 0.39, 390));
  const restaurantMenus = useMemo(() => menusMock.filter((menu) => menu.restaurant_id === restaurant?.id), [restaurant?.id]);
  const items = useMemo(() => menuItemsMock.filter((item) => restaurantMenus.some((menu) => menu.id === item.menu_id)), [restaurantMenus]);
  const categories = useMemo(() => [...new Set(items.map((item) => menuItemDisplayMock[item.id]?.category ?? 'Menu'))], [items]);
  const filteredItems = items.filter((item) => item.name.toLocaleLowerCase().includes(query.trim().toLocaleLowerCase()));
  const visibleCategories = categories.filter((category) => filteredItems.some((item) => (menuItemDisplayMock[item.id]?.category ?? 'Menu') === category));
  const coverItemId = restaurant && restaurantDisplayMock[restaurant.id]?.coverItemId;
  const cover = coverItemId ? menuItemPhotos[coverItemId] : menuItemPhotos[items[0]?.id];

  if (!restaurant) {
    return <View style={styles.notFound}><Text style={styles.restaurantName}>Restaurant not found</Text><TouchableOpacity onPress={goToRestaurants}><Text style={styles.backLink}>Back to restaurants</Text></TouchableOpacity></View>;
  }

  const goToCategory = (category: string) => {
    const sectionY = sectionOffsets.current[category];
    if (sectionY != null) scrollRef.current?.scrollTo({ y: Math.max(0, coverHeight - PANEL_OVERLAP + infoHeight + sectionY - insets.top - STICKY_HEIGHT - 12), animated: true });
  };

  //TODO: IMPLEMENT CART LOGIC
  const addItem = (_item: MenuItem) => {
    // Cart handling will be connected here.
  };

  return (
    <View style={styles.page}>
      <RestaurantCover restaurant={restaurant} source={cover} height={coverHeight} />
      <ScrollView ref={scrollRef} style={styles.scroll} contentContainerStyle={styles.scrollContent} scrollEventThrottle={16} onScroll={(event) => {
        const offset = event.nativeEvent.contentOffset.y;
        setStickyVisible(offset >= coverHeight - PANEL_OVERLAP + infoHeight - insets.top);
        setCoverControlsVisible(offset < coverHeight - PANEL_OVERLAP - insets.top - 60);
      }}>
        <View pointerEvents="none" style={{ height: coverHeight }} />
        <View style={styles.panel}>
          <View onLayout={(event) => setInfoHeight(event.nativeEvent.layout.height)}>
            <RestaurantInfoPanel restaurant={restaurant} showMore={moreOpen} />
          </View>
          <View style={styles.menu}>
            <Text style={styles.menuHeading}>Menu</Text>
            {visibleCategories.map((category) => {
              const categoryItems = filteredItems.filter((item) => (menuItemDisplayMock[item.id]?.category ?? 'Menu') === category);
              if (categoryItems.length === 0) return null;
              return <View key={category} style={styles.categorySection} onLayout={(event) => { sectionOffsets.current[category] = event.nativeEvent.layout.y; }}>
                <Text style={styles.categoryHeading}>{category}</Text>
                <View style={styles.itemGrid}>{categoryItems.map((item) => <MenuItemCard key={item.id} item={item} onAdd={addItem} />)}</View>
              </View>;
            })}
            {filteredItems.length === 0 && <Text style={styles.emptyText}>No menu items match “{query}”.</Text>}
          </View>
        </View>
      </ScrollView>
      {coverControlsVisible && !stickyVisible && <View pointerEvents="box-none" style={[styles.coverControls, { top: insets.top + 12 }]}>
        <IconButton label="Back to restaurants" symbol="‹" onPress={goToRestaurants} />
        <View style={styles.controlGroup}>
          <IconButton label="Search menu" symbol="⌕" onPress={() => setSearchOpen(true)} />
          <IconButton label={favorite ? 'Remove from favorites' : 'Add to favorites'} symbol={favorite ? '♥' : '♡'} onPress={() => setFavorite((value) => !value)} />
          <IconButton label="More restaurant information" symbol="⋯" onPress={() => setMoreOpen((value) => !value)} />
        </View>
      </View>}
      {(stickyVisible || searchOpen) && <View style={[styles.sticky, { paddingTop: insets.top }]}>
        <View style={styles.stickyTop}>
          <IconButton label="Back to restaurants" symbol="‹" onPress={goToRestaurants} light />
          {searchOpen ? <TextInput autoFocus value={query} onChangeText={setQuery} placeholder="Search menu" placeholderTextColor="#9F9092" style={styles.searchInput} returnKeyType="search" /> : <Text numberOfLines={1} style={styles.stickyName}>{restaurant.name}</Text>}
          <IconButton label={searchOpen ? 'Close search' : 'Search menu'} symbol={searchOpen ? '×' : '⌕'} onPress={() => { setSearchOpen((value) => !value); setQuery(''); }} light />
          <IconButton label={favorite ? 'Remove from favorites' : 'Add to favorites'} symbol={favorite ? '♥' : '♡'} onPress={() => setFavorite((value) => !value)} light />
        </View>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.categoryNav} contentContainerStyle={styles.categoryNavContent}>
          {visibleCategories.map((category) => <TouchableOpacity key={category} onPress={() => goToCategory(category)} style={styles.categoryTab} accessibilityRole="button"><Text style={styles.categoryTabText}>{category}</Text></TouchableOpacity>)}
        </ScrollView>
      </View>}
    </View>
  );
}
