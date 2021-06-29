import { EffectCallback, useEffect, useContext } from 'react';
import { WebsocketContext } from './web-socket-context';

/**
 * Enhanced useEffect.
 *
 * Executed when received message from server is found in deps.
 *
 * @param effect
 * @param deps
 */
export function useWebsocketEffect(effect: EffectCallback, deps: string[]) {
  const { message, setMessage } = useContext(WebsocketContext);

  useEffect(() => {
    if (deps.includes(message)) {
      setMessage('');
      return effect();
    }
    /* eslint-disable react-hooks/exhaustive-deps */
  }, [message]);
}
