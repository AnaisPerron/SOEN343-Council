import { useRef, useState } from 'react';
import {
  Image,
  type ImageSourcePropType,
  Platform,
  ScrollView,
  Text,
  TouchableOpacity,
  useWindowDimensions,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { restaurantsMock } from '../../mocks/restaurants.mock';
import { menusMock, menuItemsMock } from '../../mocks/menus.mock';
import type { RestaurantStatus } from '../../mocks/types';
import { styles } from './home.styles';

const tenderLogo = require('../../../assets/tender-logo.png');

const sections = [
  {
    title: 'Top picks for you',
    note: 'A little something for every craving',
    ids: [1, 2, 3, 4],
  },
  {
    title: 'Open near me',
    note: 'Ready when you are',
    ids: [2, 1, 3],
  },
  {
    title: 'For cheap treats',
    note: 'Good food, easy on the wallet',
    ids: [4, 2, 1],
  },
  {
    title: 'Feeling fancy?',
    note: 'Make tonight feel like an occasion',
    ids: [1, 3, 2],
  },
  {
    title: 'For your sweet tooth',
    note: 'Save room for something sweet',
    ids: [3, 4, 2],
  },
];

// Keep demo ratings tied to restaurant IDs so they stay consistent across sections.
const mockRatings: Record<number, string> = {
  1: '4.8',
  2: '4.6',
  3: '4.9',
  4: '4.7',
};

// Static requires let Metro bundle each local menu photo into the app.
const menuItemPhotos: Record<number, ImageSourcePropType> = {
  1001: require('../../mocks/menu-items-imgs/maple-glazed-chicken-sandwich.jpg'),
  1002: require('../../mocks/menu-items-imgs/truffle.jpg'),
  1003: require('../../mocks/menu-items-imgs/apple-crumble.jpg'),
  2001: require('../../mocks/menu-items-imgs/harvest-grain-bowl.jpg'),
  2002: require('../../mocks/menu-items-imgs/miso-tofu-bowl.jpg'),
  3001: require('../../mocks/menu-items-imgs/smoked-salmon-bagel.jpg'),
  4001: require('../../mocks/menu-items-imgs/margherita-pizza.jpg'),
};

export default function CustomerHome() {
  const { width } = useWindowDimensions();
  const compact = width < 600;

  return (
    <SafeAreaView style={styles.page} edges={['top']}>
      <ScrollView
        stickyHeaderIndices={[2]}
        contentContainerStyle={[
          styles.content,
          { paddingHorizontal: compact ? 20 : 48 },
        ]}
      >
        <View style={styles.topbar}>
          <View style={styles.brandLockup}>
            <Image
              source={tenderLogo}
              style={styles.logoImage}
              resizeMode="contain"
              accessibilityLabel="Tender"
            />
          </View>
          <TouchableOpacity
            style={styles.locationPill}
            accessibilityRole="button"
            accessibilityLabel="Delivery location, Montreal"
          >
            <Text style={styles.pin}>⌖</Text>
            {/*Placeholdr for now. Will need to be implemented for later phases*/}
            <View>
              <Text style={styles.locationCaption}>DELIVERING TO</Text>
              <Text style={styles.location}>
                Montreal, QC <Text style={styles.chevron}>⌄</Text>
              </Text>
            </View>
          </TouchableOpacity>
          {!compact && (
            <TouchableOpacity style={styles.profile} accessibilityRole="button">
              <Text style={styles.profileText}>A</Text>
            </TouchableOpacity>
          )}
        </View>

        <View style={styles.pointsPanel}>
          <View style={styles.pointsTop}>
            <View style={styles.pointsCopy}>
              <Text style={styles.pointsEyebrow}>TENDER CASH</Text>
              {/* Placeholder for now. TODO: Connect with actual Tender Cash */}
              <Text style={styles.pointsValue}>0<Text style={styles.pointsUnit}> pts</Text></Text>
              <Text style={styles.pointsTitle}>Your cheapest way to get lucky tonight.</Text>
            </View>
            <Image
              source={tenderLogo}
              style={styles.pointsLogo}
              resizeMode="contain"
              accessibilityLabel="Tender logo"
            />
          </View>
          <TouchableOpacity style={styles.pointsAction} accessibilityRole="button">
            {/* Empty Route for now. TODO: should lead to the swiping tinder-like page */}
            <Text style={styles.pointsActionText}>Find a yummy soulmate</Text>
            <View style={styles.pointsActionIcon}>
              <Text style={styles.pointsActionArrow}>›</Text>
            </View>
          </TouchableOpacity>
        </View>

        {/* Keep the search background opaque while rows scroll underneath it. */}
        <View style={styles.stickySearch}>
          <TouchableOpacity
            style={styles.search}
            accessibilityRole="button"
            accessibilityLabel="Search restaurants or dishes"
          >
            <Text style={styles.searchIcon}>⌕</Text>
            {/*In later phases, a */}
            <Text style={styles.searchPlaceholder}>Restaurants</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.sectionIntro}>
          <Text style={styles.sectionKicker}>THE GOOD STUFF</Text>
          <Text style={styles.introTitle}>Made for your kind of day</Text>
        </View>

        {sections.map((section, index) => {
          const items = section.ids
            .map((id) => restaurantsMock.find((restaurant) => restaurant.id === id)!)
            .filter(Boolean);

          return (
            <RestaurantSection
              key={section.title}
              title={section.title}
              note={section.note}
              items={items}
              compact={compact}
              index={index}
            />
          );
        })}
      </ScrollView>
    </SafeAreaView>
  );
}

function RestaurantSection({
  title,
  note,
  items,
  compact,
  index,
}: {
  title: string;
  note: string;
  items: typeof restaurantsMock;
  compact: boolean;
  index: number;
}) {
  const scrollRef = useRef<ScrollView>(null);
  const scrollBy = (direction: number) => {
    scrollRef.current?.scrollTo({ x: direction * 430, animated: true });
  };

  return (
    <View style={styles.section}>
      <View style={styles.sectionHeader}>
        <View style={styles.sectionTitles}>
          <Text style={styles.sectionTitle}>{title}</Text>
          <Text style={styles.sectionNote}>{note}</Text>
        </View>
        {Platform.OS === 'web' && !compact && (
          <View style={styles.arrows}>
            <TouchableOpacity
              onPress={() => scrollBy(-1)}
              style={styles.arrow}
              accessibilityRole="button"
              accessibilityLabel={`Scroll ${title} left`}
            >
              <Text style={styles.arrowText}>‹</Text>
            </TouchableOpacity>
            <TouchableOpacity
              onPress={() => scrollBy(1)}
              style={styles.arrow}
              accessibilityRole="button"
              accessibilityLabel={`Scroll ${title} right`}
            >
              <Text style={styles.arrowText}>›</Text>
            </TouchableOpacity>
          </View>
        )}
      </View>

      <ScrollView
        ref={scrollRef}
        horizontal
        showsHorizontalScrollIndicator={Platform.OS === 'web'}
        contentContainerStyle={styles.cardRow}
      >
        {items.map((restaurant, itemIndex) => (
          <RestaurantCard
            key={`${index}-${restaurant.id}-${itemIndex}`}
            name={restaurant.name}
            status={restaurant.status}
            rating={mockRatings[restaurant.id]}
            coverOptions={menuItemsMock
              .filter((menuItem) => menusMock.some((menu) =>
                menu.id === menuItem.menu_id && menu.restaurant_id === restaurant.id,
              ))
              .map((menuItem) => menuItemPhotos[menuItem.id])
              .filter((photo): photo is ImageSourcePropType => photo !== undefined)}
          />
        ))}
      </ScrollView>
    </View>
  );
}

function RestaurantCard({
  name,
  status,
  rating,
  coverOptions,
}: {
  name: string;
  status: RestaurantStatus;
  rating: string;
  coverOptions: ImageSourcePropType[];
}) {
  // Choose once per mounted card so scrolling or rerendering does not reshuffle covers.
  const [cover] = useState<ImageSourcePropType | undefined>(() =>
    coverOptions.length > 0
      ? coverOptions[Math.floor(Math.random() * coverOptions.length)]
      : undefined,
  );
  const isOpen = status === 'open';
  const statusText =
    isOpen
      ? 'Open now'
      : status === 'paused'
        ? 'Taking a short break'
        : 'Closed for now';
  const statusStyle =
    isOpen
      ? styles.statusOpen
      : status === 'paused'
        ? styles.statusPaused
        : styles.statusClosed;

  return (
    <TouchableOpacity
      style={styles.card}
      accessibilityRole="button"
      accessibilityLabel={`${name}, ${status}`}
    >
      {/* Keep a square cover frame and retain the branded fallback if no photo exists. */}
      <View style={styles.art}>
        {cover ? (
          <Image source={cover} style={styles.coverImage} resizeMode="cover" />
        ) : (
          <>
            <View style={styles.artRing}>
              <View style={styles.artCircle}>
                <View style={styles.crossLine} />
                <View style={styles.crossLineVertical} />
              </View>
            </View>
            <View style={styles.artLabel}>
              <Text style={styles.artLabelText}>PHOTO COMING SOON</Text>
            </View>
          </>
        )}
      </View>

      <View style={styles.cardMeta}>
        <View style={styles.cardTitleRow}>
          <Text numberOfLines={1} style={styles.cardTitle}>{name}</Text>
        </View>
        <Text style={[styles.cardDetail, statusStyle]}>{statusText}</Text>
        <View style={styles.ratingRow}>
          <Text style={styles.ratingStar}>★</Text>
          <Text style={styles.ratingValue}>{rating}</Text>
          {/* Review totals stay at zero until the mock data includes them. */}
          <Text style={styles.reviewCount}>(0 reviews)</Text>
        </View>
      </View>
    </TouchableOpacity>
  );
}
