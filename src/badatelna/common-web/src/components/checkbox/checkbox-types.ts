import { ReactNode } from 'react';

export interface CheckboxProps {
  form?: string;
  disabled?: boolean;
  value: boolean | null | undefined;
  onChange: (value: boolean | null) => void;

  threeState?: boolean;

  icon?: ReactNode;
  checkedIcon?: ReactNode;

  highlighted?: boolean;
}
