import { createContext } from 'react';

export interface SigningContext {
  url: string;
  reportTag: string | null;
}

export const SigningContext = createContext<SigningContext>(undefined as any);
