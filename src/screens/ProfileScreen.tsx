import { Ionicons } from '@expo/vector-icons';
import React, { useState } from 'react';
import { Alert, Image, Pressable, ScrollView, Text, View } from 'react-native';
import { FadeInUp } from '../components/anim';
import { Logo } from '../components/Logo';
import { pickPhoto } from '../services/photos';
import { formatDistance } from '../utils/geo';
import { money, total } from '../utils/cost';
import { Button, Card, Chip, Field, Header, Screen } from '../components/ui';
import { useStore } from '../store';
import { useTheme } from '../theme';
import { CURRENCIES } from '../utils/cost';

export function ProfileScreen() {
  const { c, dark } = useTheme();
  const { user, settings, updateUser, setSettings, signOut, spots, tracks } = useStore();
  const stats = [
    { icon: 'location', label: 'Spots', value: String(spots.length), color: '#F97316' },
    { icon: 'checkmark-circle', label: 'Visited', value: String(spots.filter((s) => s.visited).length), color: '#16a34a' },
    { icon: 'earth', label: 'Countries', value: String(new Set(spots.map((s) => s.country).filter(Boolean)).size), color: '#0EA5E9' },
    { icon: 'footsteps', label: 'Distance', value: formatDistance(tracks.reduce((a, t) => a + t.distance, 0)), color: '#8B5CF6' },
    { icon: 'trail-sign', label: 'Trips', value: String(tracks.length), color: '#EC4899' },
    { icon: 'wallet', label: 'Spent', value: money(spots.reduce((a, x) => a + total(x.expenses), 0), settings.currency), color: '#0D9488' },
  ];
  const [name, setName] = useState(user!.name);
  const [phone, setPhone] = useState(user!.phone ?? '');
  const dirty = name.trim() !== user!.name || phone.trim() !== (user!.phone ?? '');

  const changePhoto = async () => {
    const p = await pickPhoto();
    if (p) await updateUser({ photo: p });
  };

  const save = async () => {
    if (!name.trim()) return Alert.alert('Name required', 'Your name cannot be empty.');
    await updateUser({ name: name.trim(), phone: phone.trim() || undefined });
    Alert.alert('Saved', 'Profile updated.');
  };

  return (
    <Screen>
      <Header title="Profile" />
      <ScrollView contentContainerStyle={{ padding: 16, paddingTop: 0, paddingBottom: 40 }} keyboardShouldPersistTaps="handled">
        <View style={{ alignItems: 'center', marginBottom: 18 }}>
          <Pressable onPress={changePhoto}>
            {user!.photo ? (
              <Image source={{ uri: user!.photo }} style={{ width: 96, height: 96, borderRadius: 48 }} />
            ) : (
              <View style={{ width: 96, height: 96, borderRadius: 48, backgroundColor: c.primary, alignItems: 'center', justifyContent: 'center' }}>
                <Text style={{ color: c.onPrimary, fontSize: 38, fontWeight: '800' }}>{user!.name[0]?.toUpperCase()}</Text>
              </View>
            )}
            <View style={{ position: 'absolute', right: 0, bottom: 0, backgroundColor: c.accent, borderRadius: 16, padding: 6 }}>
              <Ionicons name="camera" size={16} color="#fff" />
            </View>
          </Pressable>
          <Text style={{ color: c.sub, marginTop: 8 }}>{user!.email} · {user!.provider === 'google' ? 'Google' : 'Email'} account</Text>
        </View>

        <FadeInUp>
          <View style={{ flexDirection: 'row', flexWrap: 'wrap', marginHorizontal: -5, marginBottom: 16 }}>
            {stats.map((st) => (
              <View key={st.label} style={{ width: '33.33%', padding: 5 }}>
                <View style={{ backgroundColor: st.color + '1F', borderRadius: 16, padding: 12, alignItems: 'center' }}>
                  <Ionicons name={st.icon as any} size={22} color={st.color} />
                  <Text style={{ color: c.text, fontWeight: '800', fontSize: 15, marginTop: 4 }} numberOfLines={1} adjustsFontSizeToFit>{st.value}</Text>
                  <Text style={{ color: c.sub, fontSize: 11 }}>{st.label}</Text>
                </View>
              </View>
            ))}
          </View>
        </FadeInUp>
        <Field label="Name" value={name} onChangeText={setName} />
        <Field label="Phone" value={phone} onChangeText={setPhone} keyboardType="phone-pad" placeholder="Optional" />
        <Button title="Save changes" icon="checkmark" onPress={save} disabled={!dirty} />

        <Text style={{ color: c.text, fontWeight: '800', fontSize: 16, marginTop: 24, marginBottom: 8 }}>Currency</Text>
        <View style={{ flexDirection: 'row', flexWrap: 'wrap' }}>
          {CURRENCIES.map((cu) => (
            <Chip key={cu.code} label={`${cu.symbol.trim()} ${cu.code}`} active={settings.currency === cu.code} onPress={() => setSettings({ currency: cu.code })} />
          ))}
        </View>

        <Text style={{ color: c.text, fontWeight: '800', fontSize: 16, marginTop: 14, marginBottom: 8 }}>Appearance</Text>
        <View style={{ flexDirection: 'row' }}>
          {(['system', 'light', 'dark'] as const).map((m) => (
            <Chip key={m} label={m} active={settings.theme === m} onPress={() => setSettings({ theme: m })} icon={m === 'dark' ? 'moon' : m === 'light' ? 'sunny' : 'phone-portrait'} />
          ))}
        </View>

        <Card style={{ marginTop: 16, flexDirection: 'row', alignItems: 'center' }}>
          <Logo size={44} variant={dark ? 'dark' : 'light'} />
          <View style={{ marginLeft: 12, flex: 1 }}>
            <Text style={{ color: c.text, fontWeight: '700' }}>Spot Reminder</Text>
            <Text style={{ color: c.sub, fontSize: 12 }}>Maps © OpenStreetMap contributors</Text>
          </View>
        </Card>

        <Button title="Sign out" kind="danger" icon="log-out-outline" onPress={() => Alert.alert('Sign out?', '', [
          { text: 'Cancel', style: 'cancel' }, { text: 'Sign out', style: 'destructive', onPress: signOut },
        ])} style={{ marginTop: 20 }} />
      </ScrollView>
    </Screen>
  );
}
