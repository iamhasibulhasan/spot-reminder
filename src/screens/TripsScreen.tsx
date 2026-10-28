import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import React, { useCallback } from 'react';
import { Alert, FlatList, Pressable, Text, View } from 'react-native';
import { FadeInUp, PulseDot } from '../components/anim';
import { Button, Card, Empty, Header, Screen } from '../components/ui';
import { useStore } from '../store';
import { useTheme } from '../theme';
import { formatDistance, formatDuration } from '../utils/geo';

export function TripsScreen() {
  const { c } = useTheme();
  const nav = useNavigation<any>();
  const { tracks, activeTrackId, refreshTracks, beginTrack, endTrack } = useStore();
  useFocusEffect(useCallback(() => { refreshTracks(); }, []));

  const toggle = async () => {
    try {
      if (activeTrackId) await endTrack();
      else await beginTrack();
    } catch (e: any) {
      Alert.alert('Cannot track', e.message);
    }
  };

  return (
    <Screen>
      <Header title="My trips" />
      <FlatList
        data={tracks}
        keyExtractor={(t) => t.id}
        contentContainerStyle={{ padding: 16, paddingTop: 0 }}
        ListHeaderComponent={
          <Button
            title={activeTrackId ? 'Stop recording' : 'Start recording a trip'}
            icon={activeTrackId ? 'stop-circle' : 'play-circle'}
            kind={activeTrackId ? 'danger' : 'primary'}
            onPress={toggle}
            style={{ marginBottom: 14 }}
          />
        }
        ListEmptyComponent={<Empty emoji="👣" icon="footsteps-outline" text="Recorded trips appear here, with the path you travelled." />}
        renderItem={({ item, index }) => (
          <FadeInUp index={index}><Pressable onPress={() => nav.navigate('TrackDetail', { id: item.id })}>
            <Card style={{ marginBottom: 10, flexDirection: 'row', alignItems: 'center' }}>
              <View style={{ width: 42, height: 42, borderRadius: 21, backgroundColor: c.primary + '22', alignItems: 'center', justifyContent: 'center' }}>
                {item.id === activeTrackId ? <PulseDot color={c.danger} /> : <Ionicons name="trail-sign" size={22} color={c.primary} />}
              </View>
              <View style={{ flex: 1, marginLeft: 12 }}>
                <Text style={{ color: c.text, fontWeight: '700' }} numberOfLines={1}>{item.name}</Text>
                <Text style={{ color: c.sub, fontSize: 12 }}>
                  {new Date(item.startedAt).toLocaleString()} · {formatDuration((item.endedAt ?? Date.now()) - item.startedAt)}
                </Text>
              </View>
              <Text style={{ color: c.text, fontWeight: '800' }}>{formatDistance(item.distance)}</Text>
            </Card>
          </Pressable></FadeInUp>
        )}
      />
    </Screen>
  );
}
