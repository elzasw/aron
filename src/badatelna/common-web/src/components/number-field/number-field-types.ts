import { TextFieldProps } from 'components/text-field/text-field-types';

export interface NumberFieldProps
  extends Omit<TextFieldProps, 'value' | 'onChange'> {
  value: number | null | undefined;
  onChange: (value: number | null) => void;

  minValue?: number;
  maxValue?: number;

  negative?: boolean;
  formated?: boolean;
}
