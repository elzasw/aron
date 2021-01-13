import React, { ReactElement } from 'react';
import { Autocomplete as EasAutocomplete } from '@eas/common-web';
import { Option } from '../../types';

interface Props {
  value: Option | Option[] | null;
  onChange: (value: Option | Option[] | null) => void;
  options: Option[];
  multiple?: boolean;
  onQueryChange: (q: string) => void;
  loading: boolean;
}

export function Autocomplete({
  options,
  value,
  onChange,
  onQueryChange,
  loading,
  multiple = false,
}: Props): ReactElement {
  const source = {
    setSearchQuery: onQueryChange,
    hasNextPage: () => false,
    isDataValid: () => true,
    setParams: () => null,
    getParams: () => ({}),
    loadMore: () => new Promise<void>(() => null),
    loading,
    reset: () => null,
    items: options,
    count: options.length,
  };
  return <EasAutocomplete {...{ value, onChange, source, multiple }} />;
}
