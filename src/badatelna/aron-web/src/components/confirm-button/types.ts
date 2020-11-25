import { ButtonProps } from '../button';

export interface Props extends ButtonProps {
  title?: string;
  text?: string;
  onConfirm?: () => void;
}
