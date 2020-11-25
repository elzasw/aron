export interface Props {
  onSearch: (params: { query: string }) => void;
  main?: boolean;
  value?: string;
}
