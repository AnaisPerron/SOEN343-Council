import { useRef } from 'react';
import {
  Image,
  Platform,
  ScrollView,
  Text,
  TouchableOpacity,
  useWindowDimensions,
  View,
} from 'react-native';
import { restaurantsMock } from '../../mocks/restaurants.mock';
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

export default function CustomerHome() {
  const { width } = useWindowDimensions();
  const compact = width < 600;

  return (
    <View style={styles.page}>
      <ScrollView
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
              <Text style={styles.pointsEyebrow}>TENDER REWARDS</Text>
              <Text style={styles.pointsValue}>0<Text style={styles.pointsUnit}> pts</Text></Text>
              <Text style={styles.pointsTitle}>Your next reward is on the way</Text>
            </View>
            <Image
              source={tenderLogo}
              style={styles.pointsLogo}
              resizeMode="contain"
              accessibilityLabel="Tender logo"
            />
          </View>
          <TouchableOpacity style={styles.pointsAction} accessibilityRole="button">
            <Text style={styles.pointsActionText}>Explore Tender Rewards</Text>
            <View style={styles.pointsActionIcon}>
              <Text style={styles.pointsActionArrow}>›</Text>
            </View>
          </TouchableOpacity>
        </View>

        <TouchableOpacity
          style={styles.search}
          accessibilityRole="button"
          accessibilityLabel="Search restaurants or dishes"
        >
          <Text style={styles.searchIcon}>⌕</Text>
          <Text style={styles.searchPlaceholder}>Restaurants, dishes, anything...</Text>

        </TouchableOpacity>

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
    </View>
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
            index={itemIndex + index}
          />
        ))}
      </ScrollView>
    </View>
  );
}

function RestaurantCard({
  name,
  status,
  index,
}: {
  name: string;
  status: string;
  index: number;
}) {
  const isOpen = status === 'open';
  const artColors = ['#F0143C', '#C8102E', '#FF1744', '#E6002D'];

  return (
    <TouchableOpacity
      style={styles.card}
      accessibilityRole="button"
      accessibilityLabel={`${name}, ${status}`}
    >
      <View style={[styles.art, { backgroundColor: artColors[index % artColors.length] }]}>
        <View style={styles.artRing}>
          <View style={styles.artCircle}>
            <View style={styles.crossLine} />
            <View style={styles.crossLineVertical} />
          </View>
        </View>
        <View style={styles.artLabel}>
          <Text style={styles.artLabelText}>PHOTO COMING SOON</Text>
        </View>
      </View>

      <View style={styles.cardMeta}>
        <View style={styles.cardTitleRow}>
          <Text numberOfLines={1} style={styles.cardTitle}>{name}</Text>
          <View style={[styles.statusDot, !isOpen && styles.statusDotClosed]} />
        </View>
        <Text style={styles.cardDetail}>
          {isOpen ? 'Open now' : status === 'paused' ? 'Taking a short break' : 'Closed for now'}
          <Text style={styles.metaDivider}> · </Text>
          20–35 min
        </Text>
        <View style={styles.cardFoot}>
          <View style={styles.rating}>
            <Text style={styles.ratingStar}>★</Text>
            <Text style={styles.ratingText}>{['4.8', '4.6', '4.9', '4.7'][index % 4]}</Text>
          </View>
          <Text style={styles.delivery}>Free delivery</Text>
        </View>
      </View>
    </TouchableOpacity>
  );
}
