export type Coord = { lat: number; lng: number };
export type TrackPoint = Coord & { t: number };

export type ExpenseCategory = 'transport' | 'food' | 'stay' | 'ticket' | 'shopping' | 'other';

export type Expense = {
  id: string;
  title: string;
  amount: number;
  category: ExpenseCategory;
  date: string; // YYYY-MM-DD
  note?: string;
};

export type SpotTravel = {
  from?: string;
  to?: string;
  startDate?: string; // YYYY-MM-DD
  endDate?: string;
  mode?: 'bus' | 'train' | 'car' | 'bike' | 'flight' | 'boat' | 'walk';
};

export type Spot = {
  id: string;
  name: string;
  district: string;
  country: string;
  lat: number;
  lng: number;
  radius: number; // metres
  notify: boolean;
  visited: boolean;
  travelers: number;
  note?: string;
  travel: SpotTravel;
  expenses: Expense[];
  createdAt: number;
};

export type Track = {
  id: string;
  name: string;
  startedAt: number;
  endedAt?: number;
  distance: number; // metres
  points: TrackPoint[];
};

export type User = {
  id: string;
  name: string;
  email: string;
  phone?: string;
  photo?: string;
  provider: 'local' | 'google';
  salt?: string;
  passwordHash?: string;
};

export type Settings = {
  theme: 'system' | 'light' | 'dark';
  currency: string;
};
