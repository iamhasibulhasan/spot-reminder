import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import * as Location from 'expo-location';
import React, { useState } from 'react';
import { Alert, KeyboardAvoidingView, Platform, Pressable, ScrollView, Switch, Text, View } from 'react-native';
import { OsmMap } from '../components/OsmMap';
import { Button, Chip, Field, Header, Screen } from '../components/ui';
import { Place, reverse, searchPlaces } from '../services/nominatim';
import { ensureLocationPermission } from '../services/tasks';
import { useStore } from '../store';
import { useTheme } from '../theme';
import { Coord } from '../types';

const RADII = [200, 500, 1000, 2000, 5000];

export function AddSpotScreen() {
  const { c } = useTheme();
  const nav = useNavigation<any>();
  const addSpot = useStore((s) => s.addSpot);

  const [name, setName] = useState('');
  const [district, setDistrict] = useState('');
  const [country, setCountry] = useState('');
  const [note, setNote] = useState('');
  const [travelers, setTravelers] = useState('1');
  const [radius, setRadius] = useState(1000);
  const [notify, setNotify] = useState(true);
  const [pick, setPick] = useState<Coord | null>(null);
  const [results, setResults] = useState<Place[]>([]);
  const [busy, setBusy] = useState(false);

  const apply = (p: Place) => {
    setPick({ lat: p.lat, lng: p.lng });
    setName(p.name);
    setDistrict(p.district);
    setCountry(p.country);
    setResults([]);
  };

  const find = async () => {
    const q = [name, district, country].filter(Boolean).join(', ');
    if (!q) return Alert.alert('Type something', 'Enter a spot name, district or country first.');
    setBusy(true);
    try {
      const r = await searchPlaces(q);
      if (!r.length) Alert.alert('Not found', 'Try a different name or tap the map to drop a pin.');
      setResults(r);
    } catch {
      Alert.alert('Search failed', 'Check your internet connection.');
    }
    setBusy(false);
  };

  const onTap = async (p: Coord) => {
    setPick(p);
    setResults([]);
    const place = await reverse(p).catch(() => null);
    if (place) {
      setName((n) => n || place.name);
      setDistrict((d) => d || place.district);
      setCountry((cn) => cn || place.country);
    }
  };

  const useMyLocation = async () => {
    if (!(await ensureLocationPermission(false))) return Alert.alert('Permission needed', 'Allow location access to use your position.');
    const l = await Location.getCurrentPositionAsync({});
    onTap({ lat: l.coords.latitude, lng: l.coords.longitude });
  };

  const save = async () => {
    if (!pick) return Alert.alert('Pick a location', 'Search for the place or tap the map to drop a pin.');
    if (!name.trim()) return Alert.alert('Name required', 'Give this spot a name.');
    await addSpot({
      name: name.trim(), district: district.trim(), country: country.trim(), lat: pick.lat, lng: pick.lng,
      radius, notify, travelers: Math.max(1, parseInt(travelers) || 1), note: note.trim() || undefined,
    });
    nav.goBack();
  };

  return (
    <Screen>
      <Header title="Add spot" onBack={() => nav.goBack()} />
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={{ flex: 1 }}>
        <ScrollView contentContainerStyle={{ padding: 16, paddingTop: 0 }} keyboardShouldPersistTaps="handled">
          <Field label="Spot name" value={name} onChangeText={setName} placeholder="e.g. Cox's Bazar Beach" />
          <View style={{ flexDirection: 'row', gap: 10 }}>
            <View style={{ flex: 1 }}><Field label="District" value={district} onChangeText={setDistrict} placeholder="Cox's Bazar" /></View>
            <View style={{ flex: 1 }}><Field label="Country" value={country} onChangeText={setCountry} placeholder="Bangladesh" /></View>
          </View>
          <View style={{ flexDirection: 'row', gap: 10, marginBottom: 10 }}>
            <Button title="Find on map" icon="search" onPress={find} loading={busy} style={{ flex: 1 }} />
            <Button title="My location" icon="navigate" kind="ghost" onPress={useMyLocation} style={{ flex: 1 }} />
          </View>

          {results.map((r, i) => (
            <Pressable key={i} onPress={() => apply(r)} style={{ flexDirection: 'row', alignItems: 'center', paddingVertical: 10, borderBottomWidth: 1, borderColor: c.border }}>
              <Ionicons name="location-outline" size={20} color={c.primary} />
              <Text style={{ color: c.text, flex: 1, marginLeft: 10, fontSize: 13 }} numberOfLines={2}>{r.label}</Text>
            </Pressable>
          ))}

          <Text style={{ color: c.sub, fontSize: 12, fontWeight: '600', marginTop: 12, marginBottom: 6 }}>
            {pick ? `Pinned at ${pick.lat.toFixed(5)}, ${pick.lng.toFixed(5)}` : 'Tap the map to drop a pin'}
          </Text>
          <OsmMap style={{ height: 240, borderRadius: 16 }} pick={pick} fitKey={pick ? `${pick.lat}${pick.lng}` : 'none'} onTap={onTap} />

          <Text style={{ color: c.sub, fontSize: 12, fontWeight: '600', marginTop: 16, marginBottom: 8 }}>Remind me when I'm within</Text>
          <View style={{ flexDirection: 'row', flexWrap: 'wrap' }}>
            {RADII.map((r) => (
              <Chip key={r} label={r < 1000 ? `${r} m` : `${r / 1000} km`} active={radius === r} onPress={() => setRadius(r)} />
            ))}
          </View>
          <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 12 }}>
            <Text style={{ color: c.text, flex: 1, fontWeight: '600' }}>Notify me near this spot</Text>
            <Switch value={notify} onValueChange={setNotify} trackColor={{ true: c.primary, false: c.border }} />
          </View>
          <Field label="Travellers (for per-person cost)" value={travelers} onChangeText={setTravelers} keyboardType="number-pad" />
          <Field label="Note" value={note} onChangeText={setNote} multiline placeholder="Anything to remember…" />
          <Button title="Save spot" icon="checkmark" onPress={save} />
        </ScrollView>
      </KeyboardAvoidingView>
    </Screen>
  );
}
