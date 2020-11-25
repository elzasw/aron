export interface ColorIntention {
  light: string;
  main: string;
  dark: string;
}

export interface ThemeProviderProps {
  primary: ColorIntention;
  editing: string;
  highlight: string;
}
