import { createContext } from 'react';

export interface HistoryContext {
  url: string;
}

export const HistoryContext = createContext<HistoryContext>(undefined as any);
