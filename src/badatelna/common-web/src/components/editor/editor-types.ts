import { ChangeEvent } from 'react';

export interface EditorProps {
  value: string | null | undefined;
  onChange?: (value: string | null) => void;
  onBlur?: () => void;
  onFocus?: () => void;

  disabled?: boolean;

  onChangeEvent?: (event: ChangeEvent<HTMLTextAreaElement>) => void;
  error?: boolean;

  height?: string | number;

  language?: string;
}
