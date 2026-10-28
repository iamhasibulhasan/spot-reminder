import { Ionicons } from '@expo/vector-icons';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { DarkTheme, DefaultTheme, NavigationContainer, createNavigationContainerRef } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import * as Notifications from 'expo-notifications';
import React, { useEffect } from 'react';
import { AddSpotScreen } from '../screens/AddSpotScreen';
import { AuthScreen } from '../screens/AuthScreen';
import { HomeScreen } from '../screens/HomeScreen';
import { MapScreen } from '../screens/MapScreen';
import { ProfileScreen } from '../screens/ProfileScreen';
import { SpotDetailScreen } from '../screens/SpotDetailScreen';
import { TrackDetailScreen } from '../screens/TrackDetailScreen';
import { TripsScreen } from '../screens/TripsScreen';
import { useStore } from '../store';
import { useTheme } from '../theme';

const Stack = createNativeStackNavigator();
const Tab = createBottomTabNavigator();
const navRef = createNavigationContainerRef<any>();

const icons: Record<string, [any, any]> = {
  Spots: ['location', 'location-outline'],
  Map: ['map', 'map-outline'],
  Trips: ['footsteps', 'footsteps-outline'],
  Profile: ['person', 'person-outline'],
};

function Tabs() {
  const { c } = useTheme();
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarActiveTintColor: c.primary,
        tabBarInactiveTintColor: c.sub,
        tabBarStyle: { backgroundColor: c.card, borderTopColor: c.border },
        tabBarIcon: ({ focused, color, size }) => <Ionicons name={icons[route.name][focused ? 0 : 1]} size={size} color={color} />,
      })}
    >
      <Tab.Screen name="Spots" component={HomeScreen} />
      <Tab.Screen name="Map" component={MapScreen} />
      <Tab.Screen name="Trips" component={TripsScreen} />
      <Tab.Screen name="Profile" component={ProfileScreen} />
    </Tab.Navigator>
  );
}

export function Navigation() {
  const { c, dark } = useTheme();
  const user = useStore((s) => s.user);

  // tapping a "you're near…" notification opens that spot
  useEffect(() => {
    const open = (spotId?: string) => spotId && navRef.isReady() && navRef.navigate('SpotDetail', { id: spotId });
    const sub = Notifications.addNotificationResponseReceivedListener((r) => open(r.notification.request.content.data?.spotId as string));
    return () => sub.remove();
  }, []);

  const base = dark ? DarkTheme : DefaultTheme;
  return (
    <NavigationContainer
      ref={navRef}
      theme={{ ...base, colors: { ...base.colors, background: c.bg, card: c.card, text: c.text, border: c.border, primary: c.primary } }}
    >
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        {user ? (
          <>
            <Stack.Screen name="Tabs" component={Tabs} />
            <Stack.Screen name="AddSpot" component={AddSpotScreen} />
            <Stack.Screen name="SpotDetail" component={SpotDetailScreen} />
            <Stack.Screen name="TrackDetail" component={TrackDetailScreen} />
          </>
        ) : (
          <Stack.Screen name="Auth" component={AuthScreen} />
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}
