import { createContext } from 'react';
import { Locale, LocaleName } from './locale-types';

export interface LocaleContext {
  locale: Locale;
  messages: Record<string, string>;

  switchLocale: (localeName: LocaleName) => Promise<void>;
  refresh: () => Promise<void>;
}

export const LocaleContext = createContext<LocaleContext>(undefined as any);
