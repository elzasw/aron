export interface DateTimeFieldProps {
  form?: string;
  value: string | null | undefined;
  onChange: (value: string | null) => void;
  onBlur?: () => void;
  onFocus?: () => void;

  disabled?: boolean;

  minDate?: string;
  maxDate?: string;
}
