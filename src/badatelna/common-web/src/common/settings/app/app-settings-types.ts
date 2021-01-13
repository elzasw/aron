export type AppSettings = Record<string, unknown>;

export interface AppSettingsProviderProps {
  url: string;
  autoInit?: boolean;
}
