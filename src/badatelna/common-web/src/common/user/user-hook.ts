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
  checkPermission?: (user: any, permission: string, state?: any) => boolean
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

  const hasPermission = useEventCallback((permission: string, state?: any) => {
    if (user !== undefined) {
      if (checkPermission) {
        return checkPermission(user, permission, state);
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

  const logoutWithoutRedirect = useEventCallback(async () => {
    const response = await abortableFetch(`${logoutUrl}`, {
      method: 'POST',
    });
    return await response.none();
  });

  const context: UserContext<User> = useMemo(
    () => ({
      user,
      hasPermission,
      isLogedIn,
      reload,
      logout,
      logoutWithoutRedirect,
    }),
    [user, hasPermission, isLogedIn, reload, logout, logoutWithoutRedirect]
  );

  return { user, context };
}
