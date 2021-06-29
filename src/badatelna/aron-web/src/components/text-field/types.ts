import { TextFieldProps } from '@material-ui/core/TextField';

interface CustomProps {
  value: string;
  onChange: (_: string) => any;
  className?: string;
  variant?: 'outlined' | 'standard' | 'filled';
}

export type Props = CustomProps & Omit<TextFieldProps, 'onChange'>;
