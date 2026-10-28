import { Expense, ExpenseCategory, Spot } from '../types';

export const CATEGORIES: { key: ExpenseCategory; label: string; icon: string; color: string }[] = [
  { key: 'transport', label: 'Transport', icon: 'bus', color: '#0EA5E9' },
  { key: 'food', label: 'Food', icon: 'restaurant', color: '#F97316' },
  { key: 'stay', label: 'Stay', icon: 'bed', color: '#8B5CF6' },
  { key: 'ticket', label: 'Tickets', icon: 'ticket', color: '#EC4899' },
  { key: 'shopping', label: 'Shopping', icon: 'bag-handle', color: '#10B981' },
  { key: 'other', label: 'Other', icon: 'ellipsis-horizontal-circle', color: '#64748B' },
];

export const catMeta = (k: ExpenseCategory) => CATEGORIES.find((c) => c.key === k) ?? CATEGORIES[5];

export const CURRENCIES = [
  { code: 'BDT', symbol: '৳' }, { code: 'USD', symbol: '$' }, { code: 'EUR', symbol: '€' },
  { code: 'GBP', symbol: '£' }, { code: 'INR', symbol: '₹' }, { code: 'JPY', symbol: '¥' },
  { code: 'AED', symbol: 'AED ' }, { code: 'SAR', symbol: 'SAR ' }, { code: 'MYR', symbol: 'RM ' },
];

export function money(n: number, code: string): string {
  const sym = CURRENCIES.find((c) => c.code === code)?.symbol ?? code + ' ';
  return `${sym}${n.toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`;
}

export const today = () => new Date().toISOString().slice(0, 10);

export function daysBetween(start?: string, end?: string): number {
  if (!start || !end) return 0;
  const a = Date.parse(start);
  const b = Date.parse(end);
  if (isNaN(a) || isNaN(b) || b < a) return 0;
  return Math.round((b - a) / 86400000) + 1;
}

export function total(expenses: Expense[]): number {
  return expenses.reduce((s, e) => s + e.amount, 0);
}

export function summarize(spot: Spot) {
  const sum = total(spot.expenses);
  const days = daysBetween(spot.travel.startDate, spot.travel.endDate);
  const byCategory = CATEGORIES.map((c) => ({
    ...c,
    amount: total(spot.expenses.filter((e) => e.category === c.key)),
  })).filter((c) => c.amount > 0);
  return {
    total: sum,
    days,
    perPerson: sum / Math.max(1, spot.travelers),
    perDay: days ? sum / days : 0,
    byCategory,
  };
}
