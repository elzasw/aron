export interface DateFieldProps {
  form?: string;
  value: string | null | undefined;
  onChange: (value: string | null) => void;
  onBlur?: () => void;
  onFocus?: () => void;

  disabled?: boolean;

  minDate?: string;

  /**
   * Allows to independently set minDate for picker component
   * default value is same as minDate
   */
  minDatePicker?: string;

  /**
   * Allows to independently set maxDate for picker component
   * default value is same as maxDate
   */
  maxDatePicker?: string;
  maxDate?: string;

  /**
   * Representation of the date, when stored as string
   *  'date' -> YYYY-MM-DD
   *  'date-time' -> ISO 8601 in UTC -> YYYY-MM-DDThh:mm:ss.sssZ
   */
  representation?: 'date' | 'date-time';
}
