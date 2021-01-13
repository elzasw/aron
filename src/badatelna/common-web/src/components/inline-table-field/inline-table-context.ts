import { createContext } from 'react';

export interface InlineTableFieldContext<OBJECT> {
  withRemove: boolean;
  initNewItem: () => OBJECT;
}

export const InlineTableFieldContext = createContext<
  InlineTableFieldContext<any>
>({
  withRemove: true,
  initNewItem: () => ({}),
});
