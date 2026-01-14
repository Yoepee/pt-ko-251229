import { createQueryKeys, mergeQueryKeys } from '@lukemorales/query-key-factory';

const auth = createQueryKeys('auth', {
  me: null,
});

const categories = createQueryKeys('categories', {
  all: null,
});

const polls = createQueryKeys('polls', {
  all: ['polls'] as const,
  detail: (id: number) => ['polls', 'detail', id] as const,
  rankings: ['polls', 'rankings'] as const,
});

const battles = createQueryKeys('battles', {
  characters: null,
  stats: null,
  state: null,
  rooms: (params?: { page?: number; size?: number }) => ['battles', 'rooms', params] as const,
  roomDetail: (id: number) => ['battles', 'rooms', 'detail', id] as const,
});

export const pollKeys = polls;
export const battleKeys = battles;
export const queryKeys = mergeQueryKeys(auth, categories, polls, battles);
