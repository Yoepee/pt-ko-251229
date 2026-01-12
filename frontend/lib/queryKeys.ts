import { createQueryKeys, mergeQueryKeys } from '@lukemorales/query-key-factory';

const auth = createQueryKeys('auth', {
  me: null,
});

const categories = createQueryKeys('categories', {
  all: null,
});

export const queryKeys = mergeQueryKeys(auth, categories);
