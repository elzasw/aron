import { ReactNode } from 'react';

export interface ButtonProps {
  label: ReactNode;
  tooltip?: ReactNode;
  disabled?: boolean;
  outlined?: boolean;
  contained?: boolean;
  onClick?: () => void;
}
