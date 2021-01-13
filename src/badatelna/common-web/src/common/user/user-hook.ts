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

export function useUser(
  meUrl: string,
  logoutUrl: string,
  checkPermission?: (user: any, permission: string) => boolean
) {
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
      if (checkPermission) {
        return checkPermission(user, permission);
      } else {
        return (
          user.authorities?.find((a) => a.authority === permission) !==
          undefined
        );
      }
    } else {
      return false;
    }
  });

  const isLogedIn = useEventCallback(() => {
    return user !== undefined;
  });

  const logout = useEventCallback(async (automatic = false) => {
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = `${logoutUrl}${automatic ? '?automatic' : ''}`;

    document.body.appendChild(form);
    form.submit();
  });

  const context: UserContext<User> = useMemo(
    () => ({
      user,
      hasPermission,
      isLogedIn,
      reload,
      logout,
    }),
    [user, hasPermission, isLogedIn, reload, logout]
  );

  return { user, context };
}
