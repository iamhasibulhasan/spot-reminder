import * as Location from 'expo-location';
import * as Notifications from 'expo-notifications';
import * as TaskManager from 'expo-task-manager';
import { Platform } from 'react-native';
import { Settings, Spot, Track } from '../types';
import { money, total } from '../utils/cost';
import { pathDistance } from '../utils/geo';
import { getJSON, keys, setJSON } from './storage';

export const GEOFENCE_TASK = 'spot-geofence';
export const TRACK_TASK = 'spot-track';

// Both tasks run headless, so they read/write AsyncStorage directly instead of the zustand store.
TaskManager.defineTask(GEOFENCE_TASK, async ({ data, error }: any) => {
  if (error || data?.eventType !== Location.GeofencingEventType.Enter) return;
  const uid = await getJSON<string | null>(keys.session, null);
  if (!uid) return;
  const spots = await getJSON<Spot[]>(keys.spots(uid), []);
  const spot = spots.find((s) => s.id === data.region?.identifier);
  if (!spot || !spot.notify) return;
  const settings = await getJSON<Settings>(keys.settings, { theme: 'system', currency: 'BDT' });
  const spent = total(spot.expenses);
  await Notifications.scheduleNotificationAsync({
    content: {
      title: `📍 You're near ${spot.name}`,
      body: [spot.district, spot.country].filter(Boolean).join(', ') + (spent ? ` · spent so far ${money(spent, settings.currency)}` : ''),
      data: { spotId: spot.id },
    },
    trigger: null,
  });
});

TaskManager.defineTask(TRACK_TASK, async ({ data, error }: any) => {
  if (error || !data?.locations) return;
  const active = await getJSON<{ userId: string; trackId: string } | null>(keys.activeTrack, null);
  if (!active) return;
  const tracks = await getJSON<Track[]>(keys.tracks(active.userId), []);
  const track = tracks.find((t) => t.id === active.trackId);
  if (!track) return;
  for (const l of data.locations as Location.LocationObject[]) {
    if ((l.coords.accuracy ?? 0) > 60) continue; // drop noisy fixes
    track.points.push({ lat: l.coords.latitude, lng: l.coords.longitude, t: l.timestamp });
  }
  track.distance = pathDistance(track.points);
  await setJSON(keys.tracks(active.userId), tracks);
});

export async function setupNotifications() {
  Notifications.setNotificationHandler({
    handleNotification: async () => ({
      shouldShowBanner: true,
      shouldShowList: true,
      shouldPlaySound: true,
      shouldSetBadge: false,
    }),
  });
  if (Platform.OS === 'android') {
    await Notifications.setNotificationChannelAsync('default', {
      name: 'Spot alerts',
      importance: Notifications.AndroidImportance.HIGH,
      vibrationPattern: [0, 250, 250, 250],
      lightColor: '#0D9488',
    });
  }
  const cur = await Notifications.getPermissionsAsync();
  if (!cur.granted) await Notifications.requestPermissionsAsync();
}

export async function ensureLocationPermission(background: boolean): Promise<boolean> {
  const fg = await Location.requestForegroundPermissionsAsync();
  if (!fg.granted) return false;
  if (!background) return true;
  const bg = await Location.requestBackgroundPermissionsAsync();
  return bg.granted;
}

/** Registers a geofence for every spot that has reminders on (Android caps this at 100). */
export async function syncGeofences(spots: Spot[]) {
  const regions = spots
    .filter((s) => s.notify && !s.visited)
    .slice(0, 100)
    .map((s) => ({
      identifier: s.id,
      latitude: s.lat,
      longitude: s.lng,
      radius: Math.max(100, s.radius),
      notifyOnEnter: true,
      notifyOnExit: false,
    }));
  const running = await Location.hasStartedGeofencingAsync(GEOFENCE_TASK).catch(() => false);
  if (!regions.length) {
    if (running) await Location.stopGeofencingAsync(GEOFENCE_TASK);
    return;
  }
  const bg = await Location.getBackgroundPermissionsAsync();
  if (!bg.granted && !(await ensureLocationPermission(true))) return;
  await Location.startGeofencingAsync(GEOFENCE_TASK, regions);
}

export async function startTracking() {
  if (!(await ensureLocationPermission(true))) throw new Error('Location permission (Allow all the time) is required');
  await Location.startLocationUpdatesAsync(TRACK_TASK, {
    accuracy: Location.Accuracy.High,
    distanceInterval: 15,
    timeInterval: 5000,
    pausesUpdatesAutomatically: false,
    showsBackgroundLocationIndicator: true,
    foregroundService: {
      notificationTitle: 'Spot Reminder',
      notificationBody: 'Recording your travel path…',
      notificationColor: '#0D9488',
    },
  });
}

export async function stopTracking() {
  if (await Location.hasStartedLocationUpdatesAsync(TRACK_TASK).catch(() => false)) {
    await Location.stopLocationUpdatesAsync(TRACK_TASK);
  }
}
