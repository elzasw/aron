import React, { useRef, PropsWithChildren } from 'react';
import { FormAnchorContext } from './form-anchor-context';

export function FormAnchorProvider({ children }: PropsWithChildren<any>) {
  const formAnchorRef = useRef<HTMLDivElement>(null);

  return (
    <FormAnchorContext.Provider value={{ formAnchorRef }}>
      <div ref={formAnchorRef} />
      {children}
    </FormAnchorContext.Provider>
  );
}
