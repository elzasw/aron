import { LocaleProviderProps, LocaleName, Locale } from './locale-types';
import { useMemo, useState, useEffect } from 'react';
import csLocale from 'date-fns/locale/cs';
import enLocale from 'date-fns/locale/en-GB';
import deLocale from 'date-fns/locale/de';
import skLocale from 'date-fns/locale/sk';
import { useEventCallback } from 'utils/event-callback-hook';
import { LocaleContext } from './locale-context';
import { callLoadTranslations } from './locale-api';
import { useLocalStorage } from 'utils/local-storage-hook';

export function useLocale(props: LocaleProviderProps) {
  const [localeName, setLocaleName] = useLocalStorage<LocaleName>(
    'localeName',
    props.defaultLocale
  );
  const [messages, setMessages] = useState<Record<string, string>>({});

  const switchLocale = useEventCallback(async (localeName: LocaleName) => {
    setLocaleName(localeName);
  });

  const loadTranslations = useEventCallback(async (lang: string) => {
    if (props.messages && props.messages[lang]) {
      setMessages(props.messages[lang]);
    } else if (props.translationsUrl) {
      const fetch = callLoadTranslations(props.translationsUrl, lang);

      const response = await fetch.response;

      if (response.status === 200) {
        const messages = await response.json();

        setMessages(messages);
      } else {
        setMessages({});
      }
    }
  });

  const refreshTranslations = useEventCallback(async () => {
    await loadTranslations(localeMap[localeName].easLanguage);
  });

  useEffect(() => {
    loadTranslations(localeMap[localeName].easLanguage);
  }, [loadTranslations, localeName]);

  const context: LocaleContext = useMemo(
    () => ({
      locale: localeMap[localeName],
      messages,
      switchLocale,
      refresh: refreshTranslations,
    }),
    [localeName, messages, switchLocale, refreshTranslations]
  );

  return { context };
}

const localeMap: Record<LocaleName, Locale> = {
  [LocaleName.cs]: {
    name: 'cs',
    dateFormat: 'dd.MM.yyyy',
    dateTimeFormat: 'dd.MM.yyyy HH:mm:ss',
    timeFormat: 'HH:mm:ss',
    dateFnsLocale: csLocale,
    intlLocale: 'cs',
    easLanguage: 'CZECH',
  },
  [LocaleName.en]: {
    name: 'en',
    dateFormat: 'dd/MM/yyyy',
    dateTimeFormat: 'dd/MM/yyyy HH:mm:ss',
    timeFormat: 'HH:mm:ss',
    dateFnsLocale: enLocale,
    intlLocale: 'en',
    easLanguage: 'ENGLISH',
  },
  [LocaleName.de]: {
    name: 'de',
    dateFormat: 'dd.MM.yyyy',
    dateTimeFormat: 'dd.MM.yyyy HH:mm:ss',
    timeFormat: 'HH:mm:ss',
    dateFnsLocale: deLocale,
    intlLocale: 'de',
    easLanguage: 'GERMAN',
  },
  [LocaleName.sk]: {
    name: 'sk',
    dateFormat: 'dd.MM.yyyy',
    dateTimeFormat: 'dd.MM.yyyy HH:mm:ss',
    timeFormat: 'HH:mm:ss',
    dateFnsLocale: skLocale,
    intlLocale: 'sk',
    easLanguage: 'SLOVAK',
  },
};
