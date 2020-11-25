import { TextFieldProps } from '@material-ui/core/TextField';

interface MyProps {
  value: string;
  onChange: (_: string) => any;
  className?: string;
  variant?: 'outlined' | 'standard' | 'filled';
}

export type Props = MyProps & TextFieldProps;
