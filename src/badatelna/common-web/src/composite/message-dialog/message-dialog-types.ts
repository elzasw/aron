import { ReactNode } from 'react';

export interface MessageDialogProps {
  title: ReactNode;
  content: ReactNode;
  onClose?: () => void;
}
