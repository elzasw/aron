import * as React from 'react';
import { InactivityProviderProps } from './inactivity-types';
import { useInactivity } from './inactivity-hook';

export function InactivityProvider({
  children,
  timeout,
}: React.PropsWithChildren<InactivityProviderProps>) {
  useInactivity(timeout);

  return <>{children}</>;
}
