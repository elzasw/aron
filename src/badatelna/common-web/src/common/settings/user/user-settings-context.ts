import { createContext } from 'react';
import { noop } from 'lodash';
import { TableSettings, CustomSettings } from './user-settings-types';

export interface UserSettingsContext {
  getTableSettings: (
    tableId: string,
    version: number
  ) => TableSettings | undefined;
  setTableSettings: (tableId: string, settings: TableSettings) => void;
  getCustomSettings: (
    key: string,
    version: number
  ) => CustomSettings | undefined;
  setCustomSettings: (key: string, settings: CustomSettings) => void;
  clear: () => Promise<void>;
  init: () => void;
}

export const UserSettingsContext = createContext<UserSettingsContext>({
  init: noop,
  getTableSettings: () => undefined,
  setTableSettings: noop,
  getCustomSettings: () => undefined,
  setCustomSettings: noop,
  clear: () => Promise.resolve(),
});
