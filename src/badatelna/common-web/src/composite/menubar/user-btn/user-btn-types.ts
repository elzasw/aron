import { ReactNode } from 'react';

export interface UserBtnAction {
  label: ReactNode;
  action?: () => void;
}

export interface UserBtnProps {
  logoutUrl: string;
  logoutSuccessUrl: string;
  actions?: UserBtnAction[];
}

export interface UserBtnItemProps {
  action: UserBtnAction;
  onClose: () => void;
}
