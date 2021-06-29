export interface ColorIntention {
  light: string;
  main: string;
  dark: string;
}

export interface ThemeProviderProps {
  primary: ColorIntention;
  secondary?: ColorIntention;
  editing: string;
  highlight: string;
  fontSize?: number;
  fontFamily?: string[];
  radius?: number;
}
