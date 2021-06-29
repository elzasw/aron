import { RefAttributes, ComponentType } from 'react';

export interface BulkAction<BulkActionHandle> {
  label: string;
  action: (handle: BulkActionHandle) => void;
  Component?: ComponentType<RefAttributes<BulkActionHandle>>;
  disableFilteredBulkAction?: boolean;
}

export interface BulkActionItemProps {
  action: BulkAction<any>;
  closeMenu: () => void;
}

export interface BulkActionButtonProps {
  disabled: boolean;
  actions: BulkAction<any>[];
}
