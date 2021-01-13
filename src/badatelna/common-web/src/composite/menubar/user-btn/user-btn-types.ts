import { ReactNode } from 'react';

export interface UserBtnAction {
  label: ReactNode;
  action?: () => void;
}

export interface UserBtnProps {
  actions?: UserBtnAction[];
}

export interface UserBtnItemProps {
  action: UserBtnAction;
  onClose: () => void;
}
