import {
  Palette as OriginalPalette,
  PaletteOptions as OriginalPaletteOptions,
} from '@material-ui/core/styles/createPalette';

declare module '@material-ui/core/styles/createPalette' {
  interface Palette extends OriginalPalette {
    editing: string;
    highlight: string;
  }

  interface PaletteOptions extends OriginalPaletteOptions {
    editing: string;
    highlight: string;
  }
}

declare module '@material-ui/core/styles/createMuiTheme' {
  interface Theme {
    eas?: {
      shadow: [string, string];
    };
  }
  // allow configuration using `createMuiTheme`
  interface ThemeOptions {
    eas?: {
      shadow: [string, string];
    };
  }
}
