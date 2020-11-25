declare module '@material-ui/core/styles/MuiThemeProvider' {
  export interface ThemeProviderProps<Theme> {
    children: React.ReactNode;
    theme: Partial<Theme> | ((outerTheme: Theme) => Theme);
  }
  export default function MuiThemeProvider<T>(
    props: ThemeProviderProps<T>
  ): React.ReactElement<ThemeProviderProps<T>>;
}
