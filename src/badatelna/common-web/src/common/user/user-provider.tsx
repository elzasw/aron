import * as React from 'react';
import { UserProviderProps } from './user-types';
import { UserContext } from './user-context';
import { useUser } from './user-hook';

export function UserProvider({
  children,
  meUrl,
  logoutUrl,
  checkPermission,
}: React.PropsWithChildren<UserProviderProps>) {
  const { context } = useUser(meUrl, logoutUrl, checkPermission);
  const [loaded, setLoaded] = React.useState(false);

  React.useEffect(() => {
    context.reload().then(() => {
      setLoaded(true);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <UserContext.Provider value={context}>
      {loaded && children}
    </UserContext.Provider>
  );
}
