import React, { ChangeEvent } from 'react';

export interface TextAreaProps {
  value: string | null | undefined;
  onChange?: (value: string | null) => void;
  onBlur?: () => void;
  onFocus?: () => void;

  disabled?: boolean;

  startAdornment?: React.ReactNode;
  endAdornment?: React.ReactNode;

  onChangeEvent?: (event: ChangeEvent<HTMLTextAreaElement>) => void;
  error?: boolean;

  minRows?: number;
  maxRows?: number;
}
