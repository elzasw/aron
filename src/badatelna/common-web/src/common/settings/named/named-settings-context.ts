import { createContext } from 'react';
import { NamedSettings } from './named-settings-types';

export interface NamedSettingsContext {
  getNamedSettings: (tag: string) => Promise<NamedSettings[]>;
  saveNamedSettings: (settings: NamedSettings) => Promise<NamedSettings>;
  deleteNamedSettings: (id: string) => Promise<void>;
  defaultTableNamedSettings: boolean;
}

export const NamedSettingsContext = createContext<NamedSettingsContext>({
  getNamedSettings: () => Promise.resolve([]),
  saveNamedSettings: () => Promise.reject(),
  deleteNamedSettings: () => Promise.reject(),
  defaultTableNamedSettings: false,
});
