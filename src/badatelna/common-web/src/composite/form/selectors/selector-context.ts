import { createContext } from 'react';

export interface SelectorContext {
  prefix?: string;
  selector?: (values: any) => any;
}

export const SelectorContext = createContext<SelectorContext>({});
