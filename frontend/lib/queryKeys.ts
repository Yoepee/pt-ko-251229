import { createQueryKeys, mergeQueryKeys } from '@lukemorales/query-key-factory';

const auth = createQueryKeys('auth', {
  me: null,
});

export const queryKeys = mergeQueryKeys(auth);
