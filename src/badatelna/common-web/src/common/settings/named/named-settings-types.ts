import { AuthoredObject } from 'common/common-types';

export interface NamedSettings extends AuthoredObject {
  settings: string;
  name: string;
  tag: string;
  shared: boolean;
}

export interface NamedSettingsProviderProps {
  url: string;
  defaultTableNamedSettings?: boolean;
}
