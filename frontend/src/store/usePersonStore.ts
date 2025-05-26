import { create } from 'zustand';
import { Person } from '../types/Person';

interface PersonState {
  person: Partial<Person>;
  setPerson: (person: Partial<Person>) => void;
  availableFathers: Person[];
  setAvailableFathers: (fathers: Person[]) => void;
  availableMothers: Person[];
  setAvailableMothers: (mothers: Person[]) => void;
  availableSpouses: Person[];
  setAvailableSpouses: (spouses: Person[]) => void;
  loading: boolean;
  setLoading: (loading: boolean) => void;
  error: string | null;
  setError: (error: string | null) => void;
}

export const usePersonStore = create<PersonState>((set) => ({
  person: {},
  setPerson: (person) => set((state) => {
    if (JSON.stringify(person) === JSON.stringify(state.person)) {
      return state;
    }
    return { person };
  }),
  availableFathers: [],
  setAvailableFathers: (fathers) => set({ availableFathers: fathers }),
  availableMothers: [],
  setAvailableMothers: (mothers) => set({ availableMothers: mothers }),
  availableSpouses: [],
  setAvailableSpouses: (spouses) => set({ availableSpouses: spouses }),
  loading: false,
  setLoading: (loading) => set({ loading }),
  error: null,
  setError: (error) => set({ error }),
})); 