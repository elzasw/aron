import { ButtonProps } from '@material-ui/core/Button';

export interface Props extends ButtonProps {
  label?: string;
  outlined?: boolean;
  contained?: boolean;
  className?: string;
  rounded?: boolean;
}
