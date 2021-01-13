import { createContext } from 'react';
import { noop } from 'lodash';
import { AppSettings } from './app-settings-types';

export interface AppSettingsContext<
  SETTINGS extends AppSettings = Record<string, unknown>
> {
  init: () => void;
  update: (settings: SETTINGS) => Promise<void>;
  settings: SETTINGS;
}

export const AppSettingsContext = createContext<AppSettingsContext>({
  init: noop,
  update: () => Promise.resolve(),
  settings: {},
});
