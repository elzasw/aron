import { useState } from 'react';
import { useDebouncedCallback } from 'use-debounce/lib';

export function useTableSearch() {
  const [searchQuery, setSearchQueryInternal] = useState('');

  /**
   * Debounces set search query.
   */
  const [setSearchQuery] = useDebouncedCallback(setSearchQueryInternal, 500);

  return { searchQuery, setSearchQuery };
}
