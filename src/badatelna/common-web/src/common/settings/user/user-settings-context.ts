import { createContext } from 'react';
import { noop } from 'lodash';
import { TableSettings } from './user-settings-types';

export interface UserSettingsContext {
  getTableSettings: (
    tableId: string,
    version: number
  ) => TableSettings | undefined;
  setTableSettings: (tableId: string, settings: TableSettings) => void;
  init: () => void;
}

export const UserSettingsContext = createContext<UserSettingsContext>({
  init: noop,
  getTableSettings: () => undefined,
  setTableSettings: noop,
});
