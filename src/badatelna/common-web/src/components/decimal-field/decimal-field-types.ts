import { TextFieldProps } from 'components/text-field/text-field-types';

export interface DecimalFieldProps
  extends Omit<TextFieldProps, 'value' | 'onChange'> {
  value: string | null | undefined;
  onChange: (value: string | null) => void;

  minValue?: number;
  maxValue?: number;

  negative?: boolean;
  formated?: boolean;
}
