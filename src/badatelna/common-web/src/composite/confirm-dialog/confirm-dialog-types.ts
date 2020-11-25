import { ReactNode } from 'react';

export interface ConfirmDialogProps {
  title: ReactNode;
  text: ReactNode;
  onConfirm: () => void;
  onCancel?: () => void;
}
