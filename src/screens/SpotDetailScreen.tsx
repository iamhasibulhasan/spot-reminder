import { Ionicons } from '@expo/vector-icons';
import { useNavigation, useRoute } from '@react-navigation/native';
import React, { useState } from 'react';
import { Alert, KeyboardAvoidingView, Modal, Platform, Pressable, ScrollView, Switch, Text, View } from 'react-native';
import { OsmMap } from '../components/OsmMap';
import { Button, Card, Chip, Empty, Field, Header, IconButton, Screen, Stat } from '../components/ui';
import { useStore } from '../store';
import { useTheme } from '../theme';
import { Expense, ExpenseCategory, Spot, SpotTravel } from '../types';
import { CATEGORIES, catMeta, money, summarize, today } from '../utils/cost';

const MODES: { key: NonNullable<SpotTravel['mode']>; icon: any }[] = [
  { key: 'bus', icon: 'bus' }, { key: 'train', icon: 'train' }, { key: 'car', icon: 'car' },
  { key: 'bike', icon: 'bicycle' }, { key: 'flight', icon: 'airplane' }, { key: 'boat', icon: 'boat' }, { key: 'walk', icon: 'walk' },
];

export function SpotDetailScreen() {
  const { c } = useTheme();
  const nav = useNavigation<any>();
  const { id } = useRoute<any>().params;
  const { spots, settings, updateSpot, removeSpot, saveExpense, removeExpense } = useStore();
  const spot = spots.find((s) => s.id === id);
  const [editing, setEditing] = useState<Partial<Expense> | null>(null);
  const [editTravel, setEditTravel] = useState(false);

  if (!spot) return null;
  const cur = settings.currency;
  const sum = summarize(spot);
  const setTravel = (patch: Partial<SpotTravel>) => updateSpot(spot.id, { travel: { ...spot.travel, ...patch } });

  const confirmDelete = () =>
    Alert.alert('Delete spot?', `"${spot.name}" and all its costs will be removed.`, [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Delete', style: 'destructive', onPress: async () => { nav.goBack(); await removeSpot(spot.id); } },
    ]);

  return (
    <Screen>
      <Header
        title={spot.name}
        onBack={() => nav.goBack()}
        right={<IconButton icon="trash-outline" color={c.danger} onPress={confirmDelete} />}
      />
      <ScrollView contentContainerStyle={{ padding: 16, paddingTop: 0, paddingBottom: 40 }}>
        <Text style={{ color: c.sub, marginBottom: 10 }}>{[spot.district, spot.country].filter(Boolean).join(', ')}</Text>
        <OsmMap style={{ height: 160, borderRadius: 16, marginBottom: 14 }} spots={[{ ...spot, sub: spot.district, radius: spot.radius }]} fitKey={spot.id} />

        <Card style={{ marginBottom: 14 }}>
          <View style={{ flexDirection: 'row', alignItems: 'center' }}>
            <Text style={{ color: c.text, flex: 1, fontWeight: '600' }}>Remind me near this spot</Text>
            <Switch value={spot.notify} onValueChange={(v) => updateSpot(spot.id, { notify: v })} trackColor={{ true: c.primary, false: c.border }} />
          </View>
          <View style={{ flexDirection: 'row', alignItems: 'center', marginTop: 8 }}>
            <Text style={{ color: c.text, flex: 1, fontWeight: '600' }}>Mark as visited</Text>
            <Switch value={spot.visited} onValueChange={(v) => updateSpot(spot.id, { visited: v })} trackColor={{ true: '#16a34a', false: c.border }} />
          </View>
          {!!spot.note && <Text style={{ color: c.sub, marginTop: 10 }}>{spot.note}</Text>}
        </Card>

        {/* Cost summary */}
        <Card style={{ marginBottom: 14 }}>
          <Text style={{ color: c.sub, fontSize: 12 }}>Total cost</Text>
          <Text style={{ color: c.text, fontSize: 30, fontWeight: '800', marginVertical: 4 }}>{money(sum.total, cur)}</Text>
          <View style={{ flexDirection: 'row', marginTop: 6 }}>
            <Stat label={`Per person (${spot.travelers})`} value={money(sum.perPerson, cur)} />
            <Stat label={sum.days ? `Per day (${sum.days}d)` : 'Per day'} value={sum.days ? money(sum.perDay, cur) : '—'} />
          </View>
          {sum.byCategory.length > 0 && (
            <View style={{ marginTop: 14 }}>
              <View style={{ flexDirection: 'row', height: 10, borderRadius: 5, overflow: 'hidden', marginBottom: 10 }}>
                {sum.byCategory.map((k) => <View key={k.key} style={{ flex: k.amount, backgroundColor: k.color }} />)}
              </View>
              {sum.byCategory.map((k) => (
                <View key={k.key} style={{ flexDirection: 'row', alignItems: 'center', marginVertical: 3 }}>
                  <View style={{ width: 10, height: 10, borderRadius: 5, backgroundColor: k.color, marginRight: 8 }} />
                  <Text style={{ color: c.text, flex: 1 }}>{k.label}</Text>
                  <Text style={{ color: c.sub, marginRight: 10 }}>{Math.round((k.amount / sum.total) * 100)}%</Text>
                  <Text style={{ color: c.text, fontWeight: '700' }}>{money(k.amount, cur)}</Text>
                </View>
              ))}
            </View>
          )}
        </Card>

        {/* Travel start / end */}
        <Card style={{ marginBottom: 14 }}>
          <View style={{ flexDirection: 'row', alignItems: 'center' }}>
            <Ionicons name="trail-sign" size={18} color={c.primary} />
            <Text style={{ color: c.text, fontWeight: '800', fontSize: 16, flex: 1, marginLeft: 8 }}>Travel</Text>
            <IconButton icon={editTravel ? 'checkmark' : 'create-outline'} color={c.primary} onPress={() => setEditTravel(!editTravel)} />
          </View>
          {editTravel ? (
            <View style={{ marginTop: 8 }}>
              <Field label="Start from" value={spot.travel.from ?? ''} onChangeText={(v) => setTravel({ from: v })} placeholder="Dhaka" />
              <Field label="Destination / end" value={spot.travel.to ?? ''} onChangeText={(v) => setTravel({ to: v })} placeholder={spot.name} />
              <View style={{ flexDirection: 'row', gap: 10 }}>
                <View style={{ flex: 1 }}><Field label="Start date" value={spot.travel.startDate ?? ''} onChangeText={(v) => setTravel({ startDate: v })} placeholder="YYYY-MM-DD" /></View>
                <View style={{ flex: 1 }}><Field label="End date" value={spot.travel.endDate ?? ''} onChangeText={(v) => setTravel({ endDate: v })} placeholder="YYYY-MM-DD" /></View>
              </View>
              <View style={{ flexDirection: 'row', gap: 8, marginBottom: 12 }}>
                <Button title="Start today" kind="ghost" onPress={() => setTravel({ startDate: today() })} style={{ flex: 1, paddingVertical: 8 }} />
                <Button title="End today" kind="ghost" onPress={() => setTravel({ endDate: today() })} style={{ flex: 1, paddingVertical: 8 }} />
              </View>
              <View style={{ flexDirection: 'row', flexWrap: 'wrap' }}>
                {MODES.map((m) => (
                  <Chip key={m.key} label={m.key} icon={m.icon} active={spot.travel.mode === m.key} onPress={() => setTravel({ mode: m.key })} />
                ))}
              </View>
              <Field
                label="Travellers" value={String(spot.travelers)} keyboardType="number-pad"
                onChangeText={(v) => updateSpot(spot.id, { travelers: Math.max(1, parseInt(v) || 1) })}
              />
            </View>
          ) : (
            <View style={{ marginTop: 8 }}>
              <Text style={{ color: c.text, fontSize: 15 }}>
                {spot.travel.from || '—'}  <Ionicons name="arrow-forward" size={14} color={c.sub} />  {spot.travel.to || spot.name}
              </Text>
              <Text style={{ color: c.sub, marginTop: 4 }}>
                {spot.travel.startDate || 'Start date not set'}  →  {spot.travel.endDate || 'End date not set'}
                {spot.travel.mode ? `  ·  ${spot.travel.mode}` : ''}
              </Text>
            </View>
          )}
        </Card>

        {/* Expenses */}
        <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 8 }}>
          <Text style={{ color: c.text, fontWeight: '800', fontSize: 18, flex: 1 }}>Expenses</Text>
          <Button title="Add cost" icon="add" onPress={() => setEditing({ category: 'transport', date: today() })} style={{ paddingVertical: 8 }} />
        </View>
        {spot.expenses.length === 0 && <Empty icon="receipt-outline" text="Add transport, food, stay, tickets — anything you spend here." />}
        {spot.expenses.map((e) => {
          const m = catMeta(e.category);
          return (
            <Pressable key={e.id} onPress={() => setEditing(e)}>
              <Card style={{ marginBottom: 8, flexDirection: 'row', alignItems: 'center' }}>
                <View style={{ width: 38, height: 38, borderRadius: 19, backgroundColor: m.color + '25', alignItems: 'center', justifyContent: 'center' }}>
                  <Ionicons name={m.icon as any} size={19} color={m.color} />
                </View>
                <View style={{ flex: 1, marginLeft: 12 }}>
                  <Text style={{ color: c.text, fontWeight: '700' }}>{e.title}</Text>
                  <Text style={{ color: c.sub, fontSize: 12 }}>{m.label} · {e.date}{e.note ? ` · ${e.note}` : ''}</Text>
                </View>
                <Text style={{ color: c.text, fontWeight: '800' }}>{money(e.amount, cur)}</Text>
              </Card>
            </Pressable>
          );
        })}
      </ScrollView>

      {editing && (
        <ExpenseModal
          initial={editing}
          onClose={() => setEditing(null)}
          onSave={async (e) => { await saveExpense(spot.id, e); setEditing(null); }}
          onDelete={editing.id ? async () => { await removeExpense(spot.id, editing.id!); setEditing(null); } : undefined}
        />
      )}
    </Screen>
  );
}

function ExpenseModal({
  initial, onClose, onSave, onDelete,
}: {
  initial: Partial<Expense>; onClose: () => void; onSave: (e: Omit<Expense, 'id'> & { id?: string }) => void; onDelete?: () => void;
}) {
  const { c } = useTheme();
  const [title, setTitle] = useState(initial.title ?? '');
  const [amount, setAmount] = useState(initial.amount ? String(initial.amount) : '');
  const [category, setCategory] = useState<ExpenseCategory>(initial.category ?? 'transport');
  const [date, setDate] = useState(initial.date ?? today());
  const [note, setNote] = useState(initial.note ?? '');

  const save = () => {
    const n = parseFloat(amount.replace(',', '.'));
    if (!n || n <= 0) return Alert.alert('Amount needed', 'Enter a cost greater than zero.');
    onSave({
      id: initial.id, title: title.trim() || catMeta(category).label, amount: n, category,
      date: /^\d{4}-\d{2}-\d{2}$/.test(date) ? date : today(), note: note.trim() || undefined,
    });
  };

  return (
    <Modal transparent animationType="slide" onRequestClose={onClose}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={{ flex: 1, justifyContent: 'flex-end', backgroundColor: '#0008' }}>
        <View style={{ backgroundColor: c.bg, borderTopLeftRadius: 24, borderTopRightRadius: 24, padding: 18, maxHeight: '90%' }}>
          <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 12 }}>
            <Text style={{ color: c.text, fontSize: 20, fontWeight: '800', flex: 1 }}>{initial.id ? 'Edit cost' : 'Add cost'}</Text>
            <IconButton icon="close" onPress={onClose} />
          </View>
          <ScrollView keyboardShouldPersistTaps="handled">
            <View style={{ flexDirection: 'row', flexWrap: 'wrap' }}>
              {CATEGORIES.map((k) => (
                <Chip key={k.key} label={k.label} icon={k.icon as any} color={k.color} active={category === k.key} onPress={() => setCategory(k.key)} />
              ))}
            </View>
            <Field label="Amount" value={amount} onChangeText={setAmount} keyboardType="decimal-pad" placeholder="0.00" autoFocus />
            <Field label="What for?" value={title} onChangeText={setTitle} placeholder="Bus ticket, lunch, hotel…" />
            <Field label="Date" value={date} onChangeText={setDate} placeholder="YYYY-MM-DD" />
            <Field label="Note" value={note} onChangeText={setNote} placeholder="Optional" />
            <Button title="Save" icon="checkmark" onPress={save} />
            {onDelete && <Button title="Delete" kind="danger" onPress={onDelete} style={{ marginTop: 10 }} />}
          </ScrollView>
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
}
