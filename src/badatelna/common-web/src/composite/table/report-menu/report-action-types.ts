import { ComponentType } from 'react';

export interface ReportAction {
  label: string;
  action: () => void;
  Component?: ComponentType;
}

export interface ReportActionItemProps {
  action: ReportAction;
  closeMenu: () => void;
}

export interface ReportActionButtonProps {
  disabled: boolean;
  tag: string | null;
}
