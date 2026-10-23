import { useState } from 'react';
import { ScrollView, Text, TouchableOpacity, View } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import { menuItemOptionsMock, menuItemsMock } from '../../../mocks/menus.mock';
import { useItemCustomization } from '../../../context/ItemCustomizationContext';
import { styles } from './customize.styles';

export default function CustomizeItem() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const { selections, saveSelection } = useItemCustomization();
  const item = menuItemsMock.find((entry) => entry.id === Number(id));
  const [draftOptionIds, setDraftOptionIds] = useState<number[]>(() => selections[Number(id)] ?? []);
  const options = menuItemOptionsMock.filter((option) => option.menu_item_id === item?.id);
  const extraCost = options.reduce((sum, option) => sum + (draftOptionIds.includes(option.id) ? option.additional_cost : 0), 0);
  const goBack = () => router.canGoBack()
    ? router.back()
    : router.replace({ pathname: '/customer/menu-item/[id]', params: { id: String(id) } });

  if (!item) {
    return <SafeAreaView style={styles.page}>
      <View style={styles.notFound}><Text style={styles.title}>Item not found</Text><TouchableOpacity onPress={() => router.replace('/customer/home')}><Text style={styles.backLink}>Back to restaurants</Text></TouchableOpacity></View>
    </SafeAreaView>;
  }

  const toggleOption = (optionId: number) => {
    // Changes stay in this page's draft until Save is pressed.
    setDraftOptionIds((current) => current.includes(optionId)
      ? current.filter((selectedId) => selectedId !== optionId)
      : [...current, optionId]);
  };

  const save = () => {
    saveSelection(item.id, draftOptionIds);
    goBack();
  };

  return (
    <SafeAreaView style={styles.page} edges={['top', 'bottom']}>
      <View style={styles.header}>
        <TouchableOpacity style={styles.backButton} accessibilityRole="button" accessibilityLabel="Back without saving" onPress={goBack}><Text style={styles.backText}>‹</Text></TouchableOpacity>
        <Text style={styles.headerTitle}>Customize</Text>
        <View style={styles.headerSpacer} />
      </View>
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.itemName}>{item.name}</Text>
        <Text style={styles.intro}>Choose any extras you would like to add.</Text>
        {options.length === 0 ? <Text style={styles.empty}>No customizations are available for this item.</Text> : options.map((option) => {
          const selected = draftOptionIds.includes(option.id);
          return <TouchableOpacity key={option.id} style={styles.optionRow} accessibilityRole="checkbox" accessibilityState={{ checked: selected }} onPress={() => toggleOption(option.id)}>
            <View style={[styles.optionCheck, selected && styles.optionCheckSelected]}><Text style={styles.optionCheckMark}>{selected ? '✓' : ''}</Text></View>
            <Text style={styles.optionName}>{option.name}</Text>
            <Text style={styles.optionCost}>+${option.additional_cost.toFixed(2)}</Text>
          </TouchableOpacity>;
        })}
        <View style={styles.summary}>
          <Text style={styles.summaryLabel}>Extras</Text>
          <Text style={styles.summaryValue}>+${extraCost.toFixed(2)}</Text>
        </View>
      </ScrollView>
      <View style={styles.footer}>
        <TouchableOpacity style={styles.saveButton} accessibilityRole="button" onPress={save}><Text style={styles.saveText}>Save</Text></TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}
