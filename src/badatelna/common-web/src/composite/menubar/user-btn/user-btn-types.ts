import { ReactNode } from 'react';

export interface UserBtnAction {
  label: ReactNode;
  href?: string;
  action?: () => void;
}

export interface UserBtnProps {
  actions?: UserBtnAction[];
  displayLogoutBtn?: boolean;
}

export interface UserBtnItemProps {
  action: UserBtnAction;
  onClose: () => void;
}
