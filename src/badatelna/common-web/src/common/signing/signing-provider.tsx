import * as React from 'react';
import { SigningProviderProps } from './signing-types';
import { SigningContext } from './signing-context';

export function SigningProvider({
  children,
  url,
  reportTag,
}: React.PropsWithChildren<SigningProviderProps>) {
  const context: SigningContext = React.useMemo(
    () => ({
      url,
      reportTag,
    }),
    [url, reportTag]
  );

  return (
    <SigningContext.Provider value={context}>
      {children}
    </SigningContext.Provider>
  );
}
