import {
  TableColumnState,
  TableFilterState,
  TableSort,
} from 'composite/table/table-types';

export interface TableSettings {
  version: number;
  columnsState?: TableColumnState[];
  sorts?: TableSort[];
  filtersState?: TableFilterState[];
  searchQuery?: string;
}

export interface UserSettings {
  tables?: Record<string, TableSettings>;
}

export interface UserSettingsProviderProps {
  url: string;
  autoInit?: boolean;
}

export interface CustomSettings {
  version: number;
}
