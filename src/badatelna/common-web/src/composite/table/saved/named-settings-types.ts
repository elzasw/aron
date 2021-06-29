import { NamedSettings } from 'common/settings/named/named-settings-types';
import { TableFilterState, TableColumnState, TableSort } from '../table-types';

export interface NamedSettingsButtonProps {
  disabled: boolean;
  tag: string;
}

export interface NamedSettingsItemProps {
  label: string;
  selected: boolean;
  onClick: () => void;
  closeMenu: () => void;
}

export interface CreateDialogProps {
  onConfirm: (settings: NamedSettings) => void;
}

export interface NamedTableSettings {
  filtersState: TableFilterState[];
  columnsState: TableColumnState[];
  sorts: TableSort[];
}
