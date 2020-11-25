import React from 'react';
import { BrowserRouter, Switch, Route } from 'react-router-dom';
import { sortBy } from 'lodash';

import {
  ThemeProvider,
  LocaleProvider,
  SnackbarProvider,
  NavigationProvider,
  LocaleName,
} from '@eas/common-web';

import { AppWrapper } from './components';
import { navigationItems, ApiUrl } from './enums';
import { useGet } from './hooks';
import { colorWhite } from './styles/constants';
import { ApuPartType, ApuPartItemType } from './types';

const sortByOrder = (arr: (ApuPartType | ApuPartItemType)[]) =>
  sortBy(arr, (o) => o.order);

function AppComponent() {
  const [apuPartTypes] = useGet(ApiUrl.APU_PART_TYPE);
  const [apuPartItemTypes] = useGet(ApiUrl.APU_PART_ITEM_TYPE);

  return (
    <AppWrapper>
      {apuPartTypes && apuPartItemTypes ? (
        <Switch>
          {navigationItems.map(
            ({ path, exact = false, Component, label }: any) => (
              <Route {...{ key: path, path, exact }}>
                <Component
                  {...{
                    path,
                    label,
                    apuPartTypes: sortByOrder(apuPartTypes as ApuPartType[]),
                    apuPartItemTypes: sortByOrder(
                      apuPartItemTypes as ApuPartItemType[]
                    ),
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
      <LocaleProvider defaultLocale={LocaleName.cs} translationsUrl="">
        <SnackbarProvider timeout={3000}>
          <BrowserRouter>
            <NavigationProvider>
              <AppComponent />
            </NavigationProvider>
          </BrowserRouter>
        </SnackbarProvider>
      </LocaleProvider>
    </ThemeProvider>
  );
}
