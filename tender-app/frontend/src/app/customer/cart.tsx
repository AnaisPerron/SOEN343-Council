import { ScrollView, Text, TouchableOpacity, View } from 'react-native';
import { useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useCart } from '../../context/CartContext';
import { cartTotal } from './cartStore';
import { menuItemOptionsMock, menuItemsMock } from '../../mocks/menus.mock';
import { restaurantsMock } from '../../mocks/restaurants.mock';
import { styles } from './cart.styles';

export default function CartScreen() {
  const router = useRouter();
  const { lines, setQuantity } = useCart();
  // The cart badge counts units, while each rendered row represents one item/extras combination.
  const count = lines.reduce((total, line) => total + line.quantity, 0);
  const goBack = () => router.canGoBack() ? router.back() : router.replace('/customer/home');

  return (
    <SafeAreaView style={styles.page} edges={['top', 'bottom']}>
      <View style={styles.header}>
        <TouchableOpacity accessibilityRole="button" accessibilityLabel="Back" onPress={goBack} style={styles.backButton}><Text style={styles.backText}>‹</Text></TouchableOpacity>
        <Text style={styles.title}>Cart</Text>
        <View style={styles.headerSpacer} />
      </View>
      <ScrollView contentContainerStyle={styles.content}>
        {lines.length === 0 ? <View style={styles.empty}>
          <Text style={styles.emptyTitle}>Your cart is empty</Text>
          <Text style={styles.emptyText}>Choose a menu item to start your order.</Text>
          <TouchableOpacity accessibilityRole="button" onPress={() => router.replace('/customer/home')} style={styles.browseButton}><Text style={styles.browseText}>Browse restaurants</Text></TouchableOpacity>
        </View> : <>
          <Text style={styles.count}>{count} {count === 1 ? 'item' : 'items'} in your cart</Text>
          {lines.map((line) => {
            // The cart stores IDs and prices; resolve fixture names only for display.
            const item = menuItemsMock.find((entry) => entry.id === line.itemId);
            const restaurant = restaurantsMock.find((entry) => entry.id === line.restaurantId);
            const selectedOptions = menuItemOptionsMock.filter((option) => line.optionIds.includes(option.id));
            if (!item) return null;
            return <View key={line.key} style={styles.line}>
              <Text style={styles.restaurantName}>{restaurant?.name ?? 'Restaurant'}</Text>
              <View style={styles.lineTop}>
                <Text style={styles.itemName}>{item.name}</Text>
                <Text style={styles.linePrice}>${(line.unitPrice * line.quantity).toFixed(2)}</Text>
              </View>
              {selectedOptions.length > 0 && <Text style={styles.options}>{selectedOptions.map((option) => option.name).join(', ')}</Text>}
              {/* The store removes this row when its quantity drops from one to zero. */}
              <View style={styles.quantityControl}>
                <TouchableOpacity accessibilityRole="button" accessibilityLabel={`Remove one ${item.name}`} onPress={() => setQuantity(line.key, line.quantity - 1)} style={styles.quantityButton}><Text style={styles.quantityButtonText}>−</Text></TouchableOpacity>
                <Text style={styles.quantityText}>{line.quantity}</Text>
                <TouchableOpacity accessibilityRole="button" accessibilityLabel={`Add one ${item.name}`} onPress={() => setQuantity(line.key, line.quantity + 1)} style={styles.quantityButton}><Text style={styles.quantityButtonText}>+</Text></TouchableOpacity>
              </View>
            </View>;
          })}
          {/* Subtotal uses saved unit prices, including extras; fees and taxes are separate. */}
          <View style={styles.totalRow}><Text style={styles.totalLabel}>Subtotal</Text><Text style={styles.totalValue}>${cartTotal(lines).toFixed(2)}</Text></View>
          <Text style={styles.totalNote}>Delivery fees and taxes are calculated later.</Text>
        </>}
      </ScrollView>
    </SafeAreaView>
  );
}
