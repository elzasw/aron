import React, { PropsWithChildren } from 'react';
import CssBaseline from '@material-ui/core/CssBaseline';
import { ThemeProvider as MuiThemeProvider } from '@material-ui/core/styles';
import { ThemeProviderProps } from './theme-types';
import { useTheme } from './theme-hook';

export function ThemeProvider({
  children,
  ...options
}: PropsWithChildren<ThemeProviderProps>) {
  const theme = useTheme(options);

  return (
    <>
      <MuiThemeProvider theme={theme}>
        <CssBaseline />
        {children}
      </MuiThemeProvider>
    </>
  );
}
