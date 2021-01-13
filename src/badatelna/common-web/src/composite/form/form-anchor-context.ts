import { createContext } from 'react';

export const FormAnchorContext = createContext<{
  formAnchorRef: React.RefObject<HTMLDivElement>;
}>(undefined as any);
