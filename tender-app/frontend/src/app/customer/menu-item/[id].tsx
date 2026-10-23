import { useState } from 'react';
import { Text, TouchableOpacity, useWindowDimensions, View } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { menuItemOptionsMock, menuItemsMock, menusMock } from '../../../mocks/menus.mock';
import { menuItemPhotos } from '../../../mocks/restaurantDisplay.mock';
import { restaurantsMock } from '../../../mocks/restaurants.mock';
import { DetailBackButton, DetailPage } from '../components/DetailPage';
import { styles } from './item.styles';

export default function MenuItemDetails() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { height: windowHeight } = useWindowDimensions();
  const [stickyVisible, setStickyVisible] = useState(false);
  const item = menuItemsMock.find((entry) => entry.id === Number(id));
  const menu = menusMock.find((entry) => entry.id === item?.menu_id);
  const restaurant = restaurantsMock.find((entry) => entry.id === menu?.restaurant_id);
  const options = menuItemOptionsMock.filter((option) => option.menu_item_id === item?.id);
  const coverHeight = Math.max(290, Math.min(windowHeight * 0.39, 390));

  if (!item || !restaurant) {
    return <View style={styles.notFound}>
      <Text style={styles.title}>Menu item not found</Text>
      <TouchableOpacity onPress={() => router.replace('/customer/home')} accessibilityRole="button">
        <Text style={styles.backLink}>Back to restaurants</Text>
      </TouchableOpacity>
    </View>;
  }

  const cover = item.photURL ? { uri: item.photURL } : menuItemPhotos[item.id];
  const goBack = () => router.canGoBack()
    ? router.back()
    : router.replace({ pathname: '/customer/restaurant/[id]', params: { id: String(restaurant.id) } });

  return (
    <DetailPage
      title={item.name}
      cover={cover}
      coverHeight={coverHeight}
      onScroll={(event) => setStickyVisible(event.nativeEvent.contentOffset.y >= coverHeight - 30 - insets.top)}
      foreground={<>
        {!stickyVisible && <View style={[styles.coverControls, { top: insets.top + 12 }]}><DetailBackButton onPress={goBack} /></View>}
        {stickyVisible && <View style={[styles.stickyHeader, { paddingTop: insets.top }]}>
          <DetailBackButton onPress={goBack} />
          <Text numberOfLines={1} style={styles.stickyTitle}>{item.name}</Text>
        </View>}
      </>}
    >
      <View style={styles.info}>
        <Text style={styles.restaurantName}>{restaurant.name}</Text>
        <Text style={styles.title}>{item.name}</Text>
        <Text style={styles.description}>{item.description}</Text>
        <Text style={styles.price}>${item.price.toFixed(2)}</Text>
        {!item.available && <Text style={styles.unavailable}>Not available right now</Text>}
      </View>
      <View style={styles.options}>
        <Text style={styles.optionsHeading}>Extras</Text>
        {options.length > 0 ? options.map((option) => (
          <View key={option.id} style={styles.optionRow}>
            <Text style={styles.optionName}>{option.name}</Text>
            <Text style={styles.optionCost}>+${option.additional_cost.toFixed(2)}</Text>
          </View>
        )) : <Text style={styles.emptyOptions}>No extras available for this item.</Text>}
      </View>
    </DetailPage>
  );
}
