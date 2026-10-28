import { Coord, Track, TrackPoint } from '../types';

const R = 6371000;
const rad = (d: number) => (d * Math.PI) / 180;

export function haversine(a: Coord, b: Coord): number {
  const dLat = rad(b.lat - a.lat);
  const dLng = rad(b.lng - a.lng);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(rad(a.lat)) * Math.cos(rad(b.lat)) * Math.sin(dLng / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(h));
}

export function pathDistance(points: Coord[]): number {
  let d = 0;
  for (let i = 1; i < points.length; i++) d += haversine(points[i - 1], points[i]);
  return d;
}

export function formatDistance(m: number): string {
  return m < 1000 ? `${Math.round(m)} m` : `${(m / 1000).toFixed(m < 10000 ? 2 : 1)} km`;
}

export function formatDuration(ms: number): string {
  const s = Math.max(0, Math.floor(ms / 1000));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  return h ? `${h}h ${m}m` : m ? `${m}m ${s % 60}s` : `${s}s`;
}

export const toCoords = (pts: TrackPoint[]): [number, number][] => pts.map((p) => [p.lat, p.lng]);

export function trackToGpx(t: Track): string {
  const pts = t.points
    .map((p) => `<trkpt lat="${p.lat}" lon="${p.lng}"><time>${new Date(p.t).toISOString()}</time></trkpt>`)
    .join('');
  const name = t.name.replace(/[<&>]/g, '');
  return `<?xml version="1.0" encoding="UTF-8"?><gpx version="1.1" creator="Spot Reminder" xmlns="http://www.topografix.com/GPX/1/1"><trk><name>${name}</name><trkseg>${pts}</trkseg></trk></gpx>`;
}

export const osmLink = (c: Coord, zoom = 15) =>
  `https://www.openstreetmap.org/?mlat=${c.lat}&mlon=${c.lng}#map=${zoom}/${c.lat}/${c.lng}`;
