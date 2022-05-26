import { Locale as DateFnsLocale } from 'date-fns';

export enum LocaleName {
  cs = 'cs',
  en = 'en',
  de = 'de',
  sk = 'sk',
  fr = 'fr',
}

export interface LocaleProviderProps {
  defaultLocale: LocaleName;
  translationsUrl?: string;
  messages?: Record<string, Record<string, string>>;
}

export interface Locale {
  name: string;
  dateFormat: string;
  timeFormat: string;
  dateTimeFormat: string;
  dateFnsLocale: DateFnsLocale;
  intlLocale: string;
  easLanguage: string;
}
