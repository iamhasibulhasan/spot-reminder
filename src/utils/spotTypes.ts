import { SpotType } from '../types';

export const SPOT_TYPES: { key: SpotType; label: string; emoji: string; colors: [string, string] }[] = [
  { key: 'beach', label: 'Beach', emoji: '🏖️', colors: ['#38BDF8', '#0369A1'] },
  { key: 'mountain', label: 'Mountain', emoji: '⛰️', colors: ['#A78BFA', '#4F46E5'] },
  { key: 'city', label: 'City', emoji: '🏙️', colors: ['#FB923C', '#C2410C'] },
  { key: 'forest', label: 'Forest', emoji: '🌲', colors: ['#4ADE80', '#15803D'] },
  { key: 'heritage', label: 'Heritage', emoji: '🕌', colors: ['#F472B6', '#BE185D'] },
  { key: 'food', label: 'Food', emoji: '🍜', colors: ['#FBBF24', '#B45309'] },
  { key: 'other', label: 'Other', emoji: '📍', colors: ['#2DD4BF', '#0F766E'] },
];

export const typeMeta = (k?: SpotType) => SPOT_TYPES.find((t) => t.key === k) ?? SPOT_TYPES[6];
