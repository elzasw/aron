import { createContext } from 'react';
import { DictionaryAutocomplete, ListSource } from 'common/common-types';

export interface ExportContext {
  url: string;
  tags: ListSource<DictionaryAutocomplete>;
  disableSync: boolean;
  disableAsync: boolean;
}

export const ExportContext = createContext<ExportContext>(undefined as any);
