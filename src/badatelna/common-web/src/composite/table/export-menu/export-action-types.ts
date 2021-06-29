import { ComponentType } from 'react';

export interface ExportAction {
  label: string;
  action: () => void;
  Component?: ComponentType;
}

export interface ExportActionItemProps {
  action: ExportAction;
  closeMenu: () => void;
}

export interface ExportActionButtonProps {
  disabled: boolean;
  tag: string | null;
}
