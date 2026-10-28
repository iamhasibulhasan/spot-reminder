import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import * as Location from 'expo-location';
import React, { useEffect, useState } from 'react';
import { FlatList, Pressable, Switch, Text, View } from 'react-native';
import { Card, Empty, Header, Screen, Stat } from '../components/ui';
import { useStore } from '../store';
import { useTheme } from '../theme';
import { Coord, Spot } from '../types';
import { money, total } from '../utils/cost';
import { formatDistance, haversine } from '../utils/geo';

export function HomeScreen() {
  const { c } = useTheme();
  const nav = useNavigation<any>();
  const { spots, user, settings, updateSpot } = useStore();
  const [me, setMe] = useState<Coord | null>(null);

  useEffect(() => {
    (async () => {
      const p = await Location.getForegroundPermissionsAsync();
      if (!p.granted) return;
      const l = await Location.getLastKnownPositionAsync();
      if (l) setMe({ lat: l.coords.latitude, lng: l.coords.longitude });
    })();
  }, [spots.length]);

  const spent = spots.reduce((s, x) => s + total(x.expenses), 0);
  const visited = spots.filter((s) => s.visited).length;

  const renderItem = ({ item }: { item: Spot }) => {
    const cost = total(item.expenses);
    return (
      <Pressable onPress={() => nav.navigate('SpotDetail', { id: item.id })}>
        <Card style={{ marginBottom: 10 }}>
          <View style={{ flexDirection: 'row', alignItems: 'center' }}>
            <View style={{ width: 42, height: 42, borderRadius: 21, backgroundColor: (item.visited ? '#16a34a' : c.accent) + '25', alignItems: 'center', justifyContent: 'center' }}>
              <Ionicons name={item.visited ? 'checkmark-circle' : 'location'} size={22} color={item.visited ? '#16a34a' : c.accent} />
            </View>
            <View style={{ flex: 1, marginLeft: 12 }}>
              <Text style={{ color: c.text, fontWeight: '700', fontSize: 16 }} numberOfLines={1}>{item.name}</Text>
              <Text style={{ color: c.sub, fontSize: 12 }} numberOfLines={1}>
                {[item.district, item.country].filter(Boolean).join(', ')}
                {me ? `  ·  ${formatDistance(haversine(me, item))} away` : ''}
              </Text>
            </View>
            <View style={{ alignItems: 'flex-end' }}>
              <Text style={{ color: c.text, fontWeight: '800' }}>{money(cost, settings.currency)}</Text>
              <Switch
                value={item.notify}
                onValueChange={(v) => updateSpot(item.id, { notify: v })}
                trackColor={{ true: c.primary, false: c.border }}
                style={{ transform: [{ scale: 0.8 }], marginRight: -6 }}
              />
            </View>
          </View>
        </Card>
      </Pressable>
    );
  };

  return (
    <Screen>
      <FlatList
        data={spots}
        keyExtractor={(s) => s.id}
        renderItem={renderItem}
        contentContainerStyle={{ padding: 16, paddingBottom: 100 }}
        ListHeaderComponent={
          <>
            <Header title={`Hi, ${user?.name.split(' ')[0] ?? 'traveller'} 👋`} />
            <Card style={{ marginBottom: 16, backgroundColor: c.primary, borderColor: c.primary }}>
              <Text style={{ color: c.onPrimary, opacity: 0.8, fontSize: 12 }}>Total travel spend</Text>
              <Text style={{ color: c.onPrimary, fontSize: 30, fontWeight: '800', marginVertical: 4 }}>{money(spent, settings.currency)}</Text>
              <View style={{ flexDirection: 'row', marginTop: 6 }}>
                <Stat label="Spots" value={String(spots.length)} color={c.onPrimary} />
                <Stat label="Visited" value={String(visited)} color={c.onPrimary} />
                <Stat label="Reminders on" value={String(spots.filter((s) => s.notify && !s.visited).length)} color={c.onPrimary} />
              </View>
            </Card>
          </>
        }
        ListEmptyComponent={<Empty icon="location-outline" text="No spots yet. Add a country, district or place and we'll remind you when you're near." />}
      />
      <Pressable
        onPress={() => nav.navigate('AddSpot')}
        style={{ position: 'absolute', right: 20, bottom: 20, backgroundColor: c.accent, borderRadius: 30, paddingHorizontal: 20, height: 56, flexDirection: 'row', alignItems: 'center', elevation: 6 }}
      >
        <Ionicons name="add" size={26} color="#fff" />
        <Text style={{ color: '#fff', fontWeight: '800', marginLeft: 4 }}>Add spot</Text>
      </Pressable>
    </Screen>
  );
}
