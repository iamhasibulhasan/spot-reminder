import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { LinearGradient } from 'expo-linear-gradient';
import * as Location from 'expo-location';
import React, { useEffect, useMemo, useState } from 'react';
import { FlatList, Image, Pressable, Switch, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { CountUp, FadeInUp, Float, PressScale } from '../components/anim';
import { SpotCover } from '../components/SpotCover';
import { Chip, Empty, Screen, Stat } from '../components/ui';
import { useStore } from '../store';
import { useTheme } from '../theme';
import { Coord, Spot } from '../types';
import { money, total } from '../utils/cost';
import { formatDistance, haversine } from '../utils/geo';

type Filter = 'all' | 'upcoming' | 'visited' | 'nearest';
const FILTERS: { key: Filter; label: string; icon: any }[] = [
  { key: 'all', label: 'All', icon: 'apps' },
  { key: 'upcoming', label: 'Upcoming', icon: 'flag' },
  { key: 'visited', label: 'Visited', icon: 'checkmark-circle' },
  { key: 'nearest', label: 'Nearest', icon: 'navigate' },
];

const greeting = () => {
  const h = new Date().getHours();
  return h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening';
};

export function HomeScreen() {
  const { c, dark } = useTheme();
  const nav = useNavigation<any>();
  const insets = useSafeAreaInsets();
  const { spots, user, settings, updateSpot } = useStore();
  const [me, setMe] = useState<Coord | null>(null);
  const [filter, setFilter] = useState<Filter>('all');
  const [q, setQ] = useState('');

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
  const dist = (s: Spot) => (me ? haversine(me, s) : Infinity);

  const nearest = useMemo(() => {
    const up = spots.filter((s) => !s.visited);
    return me && up.length ? up.reduce((a, b) => (dist(a) < dist(b) ? a : b)) : null;
  }, [spots, me]);

  const list = useMemo(() => {
    let r = spots.filter((s) => {
      if (filter === 'upcoming' && s.visited) return false;
      if (filter === 'visited' && !s.visited) return false;
      const t = q.trim().toLowerCase();
      return !t || `${s.name} ${s.district} ${s.country}`.toLowerCase().includes(t);
    });
    if (filter === 'nearest') r = [...r].sort((a, b) => dist(a) - dist(b));
    return r;
  }, [spots, filter, q, me]);

  const renderItem = ({ item, index }: { item: Spot; index: number }) => {
    const cost = total(item.expenses);
    const pct = item.budget ? Math.min(1, cost / item.budget) : 0;
    return (
      <FadeInUp index={index} style={{ paddingHorizontal: 16, marginBottom: 12 }}>
        <PressScale onPress={() => nav.navigate('SpotDetail', { id: item.id })} style={{ backgroundColor: c.card, borderRadius: 18, padding: 10, flexDirection: 'row', borderWidth: 1, borderColor: c.border }}>
          <SpotCover spot={item} height={84} radius={14} style={{ width: 84 }} />
          <View style={{ flex: 1, marginLeft: 12, justifyContent: 'center' }}>
            <Text style={{ color: c.text, fontWeight: '800', fontSize: 16 }} numberOfLines={1}>{item.name}</Text>
            <Text style={{ color: c.sub, fontSize: 12, marginTop: 2 }} numberOfLines={1}>
              📍 {[item.district, item.country].filter(Boolean).join(', ')}
            </Text>
            <View style={{ flexDirection: 'row', alignItems: 'center', marginTop: 6 }}>
              {me && <Text style={{ color: c.primary, fontSize: 12, fontWeight: '700', marginRight: 10 }}>{formatDistance(dist(item))} away</Text>}
              {item.visited && <Text style={{ color: '#16a34a', fontSize: 12, fontWeight: '700' }}>✓ Visited</Text>}
            </View>
            {!!item.budget && (
              <View style={{ height: 5, borderRadius: 3, backgroundColor: c.border, marginTop: 8, overflow: 'hidden' }}>
                <View style={{ width: `${pct * 100}%`, height: 5, backgroundColor: cost > item.budget ? c.danger : c.primary }} />
              </View>
            )}
          </View>
          <View style={{ alignItems: 'flex-end', justifyContent: 'space-between' }}>
            <Text style={{ color: c.text, fontWeight: '800' }}>{money(cost, settings.currency)}</Text>
            <Switch
              value={item.notify}
              onValueChange={(v) => updateSpot(item.id, { notify: v })}
              trackColor={{ true: c.primary, false: c.border }}
              thumbColor="#fff"
              style={{ transform: [{ scale: 0.8 }], marginRight: -6 }}
            />
          </View>
        </PressScale>
      </FadeInUp>
    );
  };

  const hero = (
    <View>
      <LinearGradient
        colors={dark ? ['#0F766E', '#0B3A5B'] : ['#14B8A6', '#0369A1']}
        start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }}
        style={{ paddingTop: insets.top + 14, paddingHorizontal: 20, paddingBottom: 24, borderBottomLeftRadius: 32, borderBottomRightRadius: 32, overflow: 'hidden' }}
      >
        <View style={{ position: 'absolute', right: -50, top: -40, width: 190, height: 190, borderRadius: 95, backgroundColor: '#FFFFFF1A' }} />
        <View style={{ position: 'absolute', left: -40, bottom: -60, width: 150, height: 150, borderRadius: 75, backgroundColor: '#FFFFFF14' }} />
        <Float amount={6} duration={2200} style={{ position: 'absolute', right: 70, top: insets.top + 70 }}>
          <Text style={{ fontSize: 34 }}>✈️</Text>
        </Float>
        <View style={{ flexDirection: 'row', alignItems: 'center' }}>
          <View style={{ flex: 1 }}>
            <Text style={{ color: '#FFFFFFCC', fontSize: 13 }}>{greeting()} 👋</Text>
            <Text style={{ color: '#fff', fontSize: 24, fontWeight: '800' }} numberOfLines={1}>{user?.name.split(' ')[0] ?? 'Traveller'}</Text>
          </View>
          <Pressable onPress={() => nav.navigate('Profile')}>
            {user?.photo ? (
              <Image source={{ uri: user.photo }} style={{ width: 46, height: 46, borderRadius: 23, borderWidth: 2, borderColor: '#fff' }} />
            ) : (
              <View style={{ width: 46, height: 46, borderRadius: 23, backgroundColor: '#FFFFFF33', alignItems: 'center', justifyContent: 'center' }}>
                <Text style={{ color: '#fff', fontWeight: '800', fontSize: 20 }}>{user?.name[0]?.toUpperCase()}</Text>
              </View>
            )}
          </Pressable>
        </View>
        <Text style={{ color: '#FFFFFFCC', fontSize: 12, marginTop: 22 }}>Total travel spend</Text>
        <CountUp value={spent} format={(n) => money(Math.round(n), settings.currency)} style={{ color: '#fff', fontSize: 38, fontWeight: '800', marginVertical: 2 }} />
        <View style={{ flexDirection: 'row', marginTop: 12 }}>
          <Stat label="Spots" value={String(spots.length)} color="#fff" />
          <Stat label="Visited" value={String(visited)} color="#fff" />
          <Stat label="Reminders on" value={String(spots.filter((s) => s.notify && !s.visited).length)} color="#fff" />
        </View>
      </LinearGradient>

      {nearest && (
        <FadeInUp style={{ paddingHorizontal: 16, marginTop: 14 }}>
          <PressScale onPress={() => nav.navigate('SpotDetail', { id: nearest.id })} style={{ flexDirection: 'row', alignItems: 'center', backgroundColor: c.accent + '1F', borderRadius: 16, padding: 12, borderWidth: 1, borderColor: c.accent + '55' }}>
            <Text style={{ fontSize: 24 }}>🧭</Text>
            <View style={{ flex: 1, marginLeft: 10 }}>
              <Text style={{ color: c.sub, fontSize: 11 }}>Nearest spot to you</Text>
              <Text style={{ color: c.text, fontWeight: '800' }} numberOfLines={1}>{nearest.name} · {formatDistance(dist(nearest))}</Text>
            </View>
            <Ionicons name="chevron-forward" size={20} color={c.accent} />
          </PressScale>
        </FadeInUp>
      )}

      <View style={{ paddingHorizontal: 16, marginTop: 14 }}>
        <View style={{ flexDirection: 'row', alignItems: 'center', backgroundColor: c.input, borderRadius: 14, paddingHorizontal: 12, marginBottom: 12, borderWidth: 1, borderColor: c.border }}>
          <Ionicons name="search" size={18} color={c.sub} />
          <TextInput value={q} onChangeText={setQ} placeholder="Search spot, district, country…" placeholderTextColor={c.sub} style={{ flex: 1, color: c.text, paddingVertical: 10, paddingHorizontal: 8 }} />
          {!!q && <Ionicons name="close-circle" size={18} color={c.sub} onPress={() => setQ('')} />}
        </View>
        <View style={{ flexDirection: 'row', flexWrap: 'wrap' }}>
          {FILTERS.map((f) => <Chip key={f.key} label={f.label} icon={f.icon} active={filter === f.key} onPress={() => setFilter(f.key)} />)}
        </View>
      </View>
    </View>
  );

  return (
    <Screen edges={[]}>
      <FlatList
        data={list}
        keyExtractor={(s) => s.id}
        renderItem={renderItem}
        contentContainerStyle={{ paddingBottom: 110 }}
        ListHeaderComponent={hero}
        ListEmptyComponent={
          <Empty
            emoji={spots.length ? '🔎' : '🗺️'}
            icon="location-outline"
            text={spots.length ? 'No spots match your filter.' : "No spots yet. Add a country, district or place and we'll remind you when you're near."}
          />
        }
      />
      <Float amount={3} duration={1400} style={{ position: 'absolute', right: 20, bottom: 20 }}>
        <Pressable
          onPress={() => nav.navigate('AddSpot')}
          style={{ backgroundColor: c.accent, borderRadius: 30, paddingHorizontal: 20, height: 56, flexDirection: 'row', alignItems: 'center', elevation: 8 }}
        >
          <Ionicons name="add" size={26} color="#fff" />
          <Text style={{ color: '#fff', fontWeight: '800', marginLeft: 4 }}>Add spot</Text>
        </Pressable>
      </Float>
    </Screen>
  );
}
