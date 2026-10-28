import { Coord } from '../types';

const BASE = 'https://nominatim.openstreetmap.org';
const headers = { 'Accept-Language': 'en', 'User-Agent': 'SpotReminderApp/1.0' };

export type Place = Coord & { name: string; district: string; country: string; label: string };

function toPlace(r: any): Place {
  const a = r.address ?? {};
  const name =
    r.name || a.tourism || a.attraction || a.amenity || a.suburb || a.city || a.town || a.village ||
    r.display_name?.split(',')[0] || 'Spot';
  const district = a.state_district || a.county || a.city || a.town || a.state || '';
  return { lat: parseFloat(r.lat), lng: parseFloat(r.lon), name, district, country: a.country ?? '', label: r.display_name };
}

export async function searchPlaces(q: string): Promise<Place[]> {
  const res = await fetch(`${BASE}/search?q=${encodeURIComponent(q)}&format=jsonv2&addressdetails=1&limit=8`, { headers });
  if (!res.ok) throw new Error('Search failed');
  return (await res.json()).map(toPlace);
}

export async function reverse(c: Coord): Promise<Place | null> {
  const res = await fetch(`${BASE}/reverse?lat=${c.lat}&lon=${c.lng}&format=jsonv2&addressdetails=1&zoom=14`, { headers });
  if (!res.ok) return null;
  const j = await res.json();
  return j?.lat ? toPlace(j) : null;
}
