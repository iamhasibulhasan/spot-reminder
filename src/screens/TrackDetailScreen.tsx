import { useNavigation, useRoute } from '@react-navigation/native';
import { File, Paths } from 'expo-file-system';
import * as Sharing from 'expo-sharing';
import React, { useMemo, useState } from 'react';
import { Alert, ScrollView, Share, View } from 'react-native';
import { OsmMap } from '../components/OsmMap';
import { Button, Card, Field, Header, IconButton, Screen, Stat } from '../components/ui';
import { useStore } from '../store';
import { useTheme } from '../theme';
import { formatDistance, formatDuration, osmLink, toCoords, trackToGpx } from '../utils/geo';

export function TrackDetailScreen() {
  const { c } = useTheme();
  const nav = useNavigation<any>();
  const { id } = useRoute<any>().params;
  const { tracks, renameTrack, removeTrack } = useStore();
  const track = tracks.find((t) => t.id === id);
  const [name, setName] = useState(track?.name ?? '');
  const paths = useMemo(() => (track ? [{ coords: toCoords(track.points) }] : []), [track?.points.length]);
  if (!track) return null;

  const ms = (track.endedAt ?? Date.now()) - track.startedAt;
  const hours = ms / 3600000;
  const avg = hours > 0 ? track.distance / 1000 / hours : 0;
  const start = track.points[0];

  const shareGpx = async () => {
    try {
      const file = new File(Paths.cache, `${track.name.replace(/[^\w-]+/g, '_') || 'trip'}.gpx`);
      if (file.exists) file.delete();
      file.create();
      file.write(trackToGpx(track));
      if (!(await Sharing.isAvailableAsync())) return Alert.alert('Sharing not available on this device');
      await Sharing.shareAsync(file.uri, { mimeType: 'application/gpx+xml', dialogTitle: 'Share path (GPX)' });
    } catch (e: any) {
      Alert.alert('Could not export', e.message);
    }
  };

  return (
    <Screen>
      <Header
        title={track.name}
        onBack={() => nav.goBack()}
        right={
          <IconButton
            icon="trash-outline" color={c.danger}
            onPress={() => Alert.alert('Delete trip?', 'The recorded path will be removed.', [
              { text: 'Cancel', style: 'cancel' },
              { text: 'Delete', style: 'destructive', onPress: () => { nav.goBack(); removeTrack(track.id); } },
            ])}
          />
        }
      />
      <ScrollView contentContainerStyle={{ padding: 16, paddingTop: 0 }}>
        <OsmMap style={{ height: 320, borderRadius: 16, marginBottom: 14 }} paths={paths} fitKey={`${track.id}-${track.points.length}`} />
        <Card style={{ marginBottom: 14 }}>
          <View style={{ flexDirection: 'row' }}>
            <Stat label="Distance" value={formatDistance(track.distance)} />
            <Stat label="Duration" value={formatDuration(ms)} />
            <Stat label="Avg speed" value={`${avg.toFixed(1)} km/h`} />
          </View>
          <View style={{ flexDirection: 'row', marginTop: 12 }}>
            <Stat label="Started" value={new Date(track.startedAt).toLocaleString()} />
            <Stat label="Points" value={String(track.points.length)} />
          </View>
        </Card>
        <Field label="Trip name" value={name} onChangeText={setName} onEndEditing={() => name.trim() && renameTrack(track.id, name.trim())} />
        <View style={{ flexDirection: 'row', gap: 10 }}>
          <Button title="Share" icon="share-social" onPress={() => Share.share({
            message: `🗺️ ${track.name}\nDistance: ${formatDistance(track.distance)} · Time: ${formatDuration(ms)}${start ? `\nStart: ${osmLink(start)}` : ''}\n— Spot Reminder`,
          })} style={{ flex: 1 }} />
          <Button title="Export GPX" icon="download" kind="ghost" onPress={shareGpx} style={{ flex: 1 }} />
        </View>
      </ScrollView>
    </Screen>
  );
}
