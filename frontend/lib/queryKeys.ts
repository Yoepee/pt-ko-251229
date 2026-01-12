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

export const pollKeys = polls;
export const queryKeys = mergeQueryKeys(auth, categories, polls);
