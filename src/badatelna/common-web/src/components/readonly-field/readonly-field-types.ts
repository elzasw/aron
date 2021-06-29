import React from 'react';

export interface ReadonlyFieldProps {
  value: string | null | undefined;

  startAdornment?: React.ReactNode;
  endAdornment?: React.ReactNode;

  error?: boolean;

  dataMapper?: (data: any) => string;
}
