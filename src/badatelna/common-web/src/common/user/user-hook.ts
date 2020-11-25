import { useState, useMemo } from 'react';
import { User } from './user-types';
import { abortableFetch } from 'utils/abortable-fetch';
import { useEventCallback } from 'utils/event-callback-hook';
import { UserContext } from './user-context';

/**
 * Me call
 *
 * @param meUrl Url of Me service
 */
export function meCall(meUrl: string) {
  return abortableFetch(meUrl, {
    method: 'GET',
  });
}

export function useUser(meUrl: string) {
  const [user, setUser] = useState<User | undefined>(undefined);

  const reload = useEventCallback(async () => {
    const response = meCall(meUrl);

    try {
      const user = (await response.json()) as User;
      setUser(user);
    } catch (e) {
      setUser(undefined);
    }
  });

  const hasPermission = useEventCallback((permission: string) => {
    if (user !== undefined) {
      return (
        user.authorities.find((a) => a.authority === permission) !== undefined
      );
    } else {
      return false;
    }
  });

  const isLogedIn = useEventCallback(() => {
    return user !== undefined;
  });

  const context: UserContext = useMemo(
    () => ({
      user,
      hasPermission,
      isLogedIn,
      reload,
    }),
    [user, hasPermission, isLogedIn, reload]
  );

  return { user, context };
}
