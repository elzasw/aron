import { ButtonProps } from '@material-ui/core/Button';
import { ReactChild } from 'react';

export interface Props extends ButtonProps {
  label?: ReactChild;
  outlined?: boolean;
  contained?: boolean;
  className?: string;
  rounded?: boolean;
}
