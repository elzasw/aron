import { useIdleTimer } from 'react-idle-timer';
import { useContext } from 'react';
import { UserContext } from 'common/user/user-context';
import { useEventCallback } from 'utils/event-callback-hook';

export function useInactivity(timeout: number) {
  const { logout, isLogedIn } = useContext(UserContext);

  const handleOnIdle = useEventCallback(() => {
    if (isLogedIn()) {
      logout(true);
    }
  });

  useIdleTimer({
    timeout: timeout * 1000,
    onIdle: handleOnIdle,
    debounce: 500,
  });
}
