import * as Crypto from 'expo-crypto';
import { create } from 'zustand';
import { loadUsers } from '../services/auth';
import { getJSON, keys, removeKey, setJSON } from '../services/storage';
import { startTracking, stopTracking, syncGeofences } from '../services/tasks';
import { Expense, Settings, Spot, Track, User } from '../types';
import { pathDistance } from '../utils/geo';

type NewSpot = Pick<Spot, 'name' | 'district' | 'country' | 'lat' | 'lng' | 'radius' | 'notify' | 'travelers' | 'note'>;

type State = {
  ready: boolean;
  user: User | null;
  spots: Spot[];
  tracks: Track[];
  settings: Settings;
  activeTrackId: string | null;

  init: () => Promise<void>;
  signIn: (u: User) => Promise<void>;
  signOut: () => Promise<void>;
  updateUser: (patch: Partial<User>) => Promise<void>;
  setSettings: (patch: Partial<Settings>) => Promise<void>;

  addSpot: (s: NewSpot) => Promise<string>;
  updateSpot: (id: string, patch: Partial<Spot>) => Promise<void>;
  removeSpot: (id: string) => Promise<void>;
  saveExpense: (spotId: string, e: Omit<Expense, 'id'> & { id?: string }) => Promise<void>;
  removeExpense: (spotId: string, expenseId: string) => Promise<void>;

  refreshTracks: () => Promise<void>;
  beginTrack: (name?: string) => Promise<void>;
  endTrack: () => Promise<void>;
  renameTrack: (id: string, name: string) => Promise<void>;
  removeTrack: (id: string) => Promise<void>;
};

const defaultSettings: Settings = { theme: 'system', currency: 'BDT' };
const uid = () => Crypto.randomUUID();

export const useStore = create<State>((set, get) => {
  const saveSpots = async (spots: Spot[]) => {
    set({ spots });
    await setJSON(keys.spots(get().user!.id), spots);
    syncGeofences(spots).catch(() => {});
  };
  const saveTracks = async (tracks: Track[]) => {
    set({ tracks });
    await setJSON(keys.tracks(get().user!.id), tracks);
  };
  const loadUserData = async (user: User) => {
    const spots = await getJSON<Spot[]>(keys.spots(user.id), []);
    const tracks = await getJSON<Track[]>(keys.tracks(user.id), []);
    const active = await getJSON<{ userId: string; trackId: string } | null>(keys.activeTrack, null);
    set({ user, spots, tracks, activeTrackId: active?.userId === user.id ? active.trackId : null });
    syncGeofences(spots).catch(() => {});
  };

  return {
    ready: false,
    user: null,
    spots: [],
    tracks: [],
    settings: defaultSettings,
    activeTrackId: null,

    init: async () => {
      const settings = await getJSON<Settings>(keys.settings, defaultSettings);
      set({ settings });
      const session = await getJSON<string | null>(keys.session, null);
      const user = session ? (await loadUsers()).find((u) => u.id === session) : undefined;
      if (user) await loadUserData(user);
      set({ ready: true });
    },

    signIn: async (user) => {
      await setJSON(keys.session, user.id);
      await loadUserData(user);
    },

    signOut: async () => {
      if (get().activeTrackId) await get().endTrack();
      await removeKey(keys.session);
      syncGeofences([]).catch(() => {});
      set({ user: null, spots: [], tracks: [], activeTrackId: null });
    },

    updateUser: async (patch) => {
      const user = { ...get().user!, ...patch };
      const users = (await loadUsers()).map((u) => (u.id === user.id ? user : u));
      await setJSON(keys.users, users);
      set({ user });
    },

    setSettings: async (patch) => {
      const settings = { ...get().settings, ...patch };
      set({ settings });
      await setJSON(keys.settings, settings);
    },

    addSpot: async (s) => {
      const spot: Spot = {
        ...s, id: uid(), visited: false, travel: {}, expenses: [], createdAt: Date.now(),
      };
      await saveSpots([spot, ...get().spots]);
      return spot.id;
    },

    updateSpot: (id, patch) => saveSpots(get().spots.map((s) => (s.id === id ? { ...s, ...patch } : s))),

    removeSpot: (id) => saveSpots(get().spots.filter((s) => s.id !== id)),

    saveExpense: (spotId, e) =>
      saveSpots(
        get().spots.map((s) => {
          if (s.id !== spotId) return s;
          const expense = { ...e, id: e.id ?? uid() };
          const exists = s.expenses.some((x) => x.id === expense.id);
          return {
            ...s,
            expenses: exists ? s.expenses.map((x) => (x.id === expense.id ? expense : x)) : [expense, ...s.expenses],
          };
        }),
      ),

    removeExpense: (spotId, expenseId) =>
      saveSpots(
        get().spots.map((s) => (s.id === spotId ? { ...s, expenses: s.expenses.filter((x) => x.id !== expenseId) } : s)),
      ),

    refreshTracks: async () => {
      const user = get().user;
      if (user) set({ tracks: await getJSON<Track[]>(keys.tracks(user.id), []) });
    },

    beginTrack: async (name) => {
      const user = get().user!;
      await startTracking();
      const now = Date.now();
      const track: Track = {
        id: uid(),
        name: name?.trim() || `Trip ${new Date(now).toLocaleDateString()}`,
        startedAt: now,
        distance: 0,
        points: [],
      };
      await saveTracks([track, ...get().tracks]);
      await setJSON(keys.activeTrack, { userId: user.id, trackId: track.id });
      set({ activeTrackId: track.id });
    },

    endTrack: async () => {
      await stopTracking();
      await get().refreshTracks(); // pick up points written by the background task
      const id = get().activeTrackId;
      await saveTracks(
        get().tracks.map((t) => (t.id === id ? { ...t, endedAt: Date.now(), distance: pathDistance(t.points) } : t)),
      );
      await removeKey(keys.activeTrack);
      set({ activeTrackId: null });
    },

    renameTrack: (id, name) => saveTracks(get().tracks.map((t) => (t.id === id ? { ...t, name } : t))),

    removeTrack: (id) => saveTracks(get().tracks.filter((t) => t.id !== id)),
  };
});
