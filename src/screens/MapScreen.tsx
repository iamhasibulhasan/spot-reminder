import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import * as Location from 'expo-location';
import React, { useEffect, useMemo, useState } from 'react';
import { Alert, Pressable, Text, View } from 'react-native';
import { OsmMap } from '../components/OsmMap';
import { Screen } from '../components/ui';
import { ensureLocationPermission } from '../services/tasks';
import { useStore } from '../store';
import { useTheme } from '../theme';
import { Coord } from '../types';
import { formatDistance, toCoords } from '../utils/geo';

export function MapScreen() {
  const { c } = useTheme();
  const nav = useNavigation<any>();
  const { spots, tracks, activeTrackId, refreshTracks, beginTrack, endTrack } = useStore();
  const [me, setMe] = useState<Coord | null>(null);
  const [fit, setFit] = useState(0);

  const active = tracks.find((t) => t.id === activeTrackId);

  // follow the user and pull in points the background task has recorded
  useEffect(() => {
    let sub: Location.LocationSubscription | undefined;
    (async () => {
      if (!(await ensureLocationPermission(false))) return;
      sub = await Location.watchPositionAsync({ accuracy: Location.Accuracy.Balanced, distanceInterval: 10 }, (l) =>
        setMe({ lat: l.coords.latitude, lng: l.coords.longitude }),
      );
    })();
    return () => sub?.remove();
  }, []);
  useEffect(() => {
    if (!activeTrackId) return;
    const i = setInterval(refreshTracks, 4000);
    return () => clearInterval(i);
  }, [activeTrackId]);

  const mapSpots = useMemo(
    () => spots.map((s) => ({ id: s.id, name: s.name, lat: s.lat, lng: s.lng, sub: s.district, radius: s.radius, visited: s.visited })),
    [spots],
  );
  const paths = useMemo(() => (active ? [{ coords: toCoords(active.points), color: '#F97316' }] : []), [active?.points.length, active?.id]);

  const toggle = async () => {
    try {
      if (activeTrackId) await endTrack();
      else await beginTrack();
    } catch (e: any) {
      Alert.alert('Cannot track', e.message);
    }
  };

  return (
    <Screen edges={[]}>
      <OsmMap
        style={{ flex: 1 }}
        spots={mapSpots}
        paths={paths}
        me={me}
        fitKey={fit}
        onSpotPress={(id) => nav.navigate('SpotDetail', { id })}
      />
      <Pressable
        onPress={() => setFit((f) => f + 1)}
        style={{ position: 'absolute', right: 16, top: 50, backgroundColor: c.card, padding: 12, borderRadius: 24, elevation: 4 }}
      >
        <Ionicons name="locate" size={22} color={c.primary} />
      </Pressable>
      <View style={{ position: 'absolute', left: 16, right: 16, bottom: 16, backgroundColor: c.card, borderRadius: 18, padding: 14, flexDirection: 'row', alignItems: 'center', elevation: 6 }}>
        <View style={{ flex: 1 }}>
          <Text style={{ color: c.text, fontWeight: '800' }}>{active ? 'Recording your path' : 'Track my travel path'}</Text>
          <Text style={{ color: c.sub, fontSize: 12 }}>
            {active ? `${formatDistance(active.distance)} · ${active.points.length} points` : 'Start a trip to record where you go'}
          </Text>
        </View>
        <Pressable onPress={toggle} style={{ backgroundColor: active ? c.danger : c.primary, borderRadius: 12, paddingHorizontal: 16, paddingVertical: 10 }}>
          <Text style={{ color: active ? '#fff' : c.onPrimary, fontWeight: '800' }}>{active ? 'Stop' : 'Start'}</Text>
        </Pressable>
      </View>
    </Screen>
  );
}
