import React, { ChangeEvent, KeyboardEvent } from 'react';

export interface TextFieldProps {
  form?: string;
  name?: string;
  id?: string;
  value: string | null | undefined;
  onChange?: (value: string | null) => void;
  onBlur?: () => void;
  onFocus?: () => void;
  onKeyDown?: (e: KeyboardEvent) => void;

  disabled?: boolean;

  startAdornment?: React.ReactNode;
  endAdornment?: React.ReactNode;

  onChangeEvent?: (event: ChangeEvent<HTMLInputElement>) => void;
  error?: boolean;

  autocomplete?: string;
  type?: string;
}
