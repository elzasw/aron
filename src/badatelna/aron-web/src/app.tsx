import React from 'react';
import { BrowserRouter, Switch, Route } from 'react-router-dom';

import {
  ThemeProvider,
  LocaleProvider,
  SnackbarProvider,
  NavigationProvider,
  LocaleName,
} from '@eas/common-web';

import { AppWrapper } from './components';
import { navigationItems, ApiUrl, messages } from './enums';
import { useGet, AppStateProvider } from './common-utils';
import { colorWhite } from './styles/constants';

function AppComponent() {
  const [apuPartTypes] = useGet(ApiUrl.APU_PART_TYPE);
  const [apuPartItemTypes] = useGet(ApiUrl.APU_PART_ITEM_TYPE);
  const [facets] = useGet(ApiUrl.FACETS);

  return (
    <AppWrapper>
      {apuPartTypes && apuPartItemTypes && facets ? (
        <Switch>
          {navigationItems.map(
            ({ path, exact = false, Component, label }: any) => (
              <Route {...{ key: path, path, exact }}>
                <Component
                  {...{
                    path,
                    label,
                    apuPartTypes,
                    apuPartItemTypes,
                    facets,
                  }}
                />
              </Route>
            )
          )}
        </Switch>
      ) : (
        <></>
      )}
    </AppWrapper>
  );
}

export function App() {
  const primary = {
    light: '#70a0c9',
    main: '#306ea4',
    dark: '#1a3a56',
  };
  const highlight = '#feb28a';

  return (
    <ThemeProvider primary={primary} editing={colorWhite} highlight={highlight}>
      <LocaleProvider
        defaultLocale={LocaleName.cs}
        messages={messages}
        translationsUrl=""
      >
        <SnackbarProvider timeout={3000}>
          <AppStateProvider>
            <BrowserRouter {...{ basename: process.env.URL_PREFIX }}>
              <NavigationProvider>
                <AppComponent />
              </NavigationProvider>
            </BrowserRouter>
          </AppStateProvider>
        </SnackbarProvider>
      </LocaleProvider>
    </ThemeProvider>
  );
}
