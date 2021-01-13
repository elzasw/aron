export interface TimeFieldProps {
  form?: string;
  value: string | null | undefined;
  onChange: (value: string | null) => void;
  onBlur?: () => void;
  onFocus?: () => void;

  disabled?: boolean;

  minTime?: string;
  maxTime?: string;
}
