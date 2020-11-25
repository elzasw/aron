import * as React from 'react';
import { IntlProvider } from 'react-intl';
import { LocaleProviderProps } from './locale-types';
import { LocaleContext } from './locale-context';
import { useLocale } from './locale-hook';

export function LocaleProvider({
  children,
  ...props
}: React.PropsWithChildren<LocaleProviderProps>) {
  const { context } = useLocale(props);

  return (
    <IntlProvider
      locale={context.locale.intlLocale}
      defaultLocale="cs"
      messages={context.messages}
    >
      <LocaleContext.Provider value={context}>
        {children}
      </LocaleContext.Provider>
    </IntlProvider>
  );
}
