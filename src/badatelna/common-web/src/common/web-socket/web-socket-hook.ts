import { useEffect, useRef, useMemo, useState } from 'react';
import {
  Client,
  IMessage,
  StompSubscription,
  messageCallbackType,
} from '@stomp/stompjs';
import { WebsocketContext } from './web-socket-context';
import { useEventCallback } from 'utils/event-callback-hook';
import { noop } from 'lodash';

export function useWebsocket({
  wsUrl,
  destinations,
  debug,
}: {
  wsUrl: string;
  destinations: string[];
  debug: boolean;
}) {
  /**
   * Reference to websocket client.
   */
  const client = useRef<Client>();

  /**
   * Array of subscription references.
   */
  const subscribtions = useRef<StompSubscription[]>([]);

  /**
   * True if connection has been successfully established.
   */
  const [isWsActive, setIsWsActive] = useState(false);

  /**
   * Received message useWebsocketEffect reacts to.
   */
  const [message, setMessage] = useState<string>('');

  /**
   * Subscribe method.
   */
  const subscribe = useEventCallback(
    (destination: string, callback?: messageCallbackType) => {
      const subscription = client.current?.subscribe(
        destination,
        (msg: IMessage) => {
          if (callback) {
            callback(msg);
          } else {
            setMessage(msg.body);
          }
        }
      );

      if (subscription) {
        subscribtions.current.push(subscription);
      }

      return subscription;
    }
  );

  /**
   * Unsubscribe method.
   */
  const unsubscribe = useEventCallback((subscription: StompSubscription) => {
    if (subscription) {
      // console.log('unsubscribing', subscription, subscribtions);
      subscription?.unsubscribe();

      subscribtions.current = subscribtions.current.filter(
        (s) => s !== subscription
      );
    }
  });

  /**
   * Establish connection on mount && clear on unmount.
   */
  useEffect(() => {
    // construct full url
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const fullUrl = `${protocol}//${window.location.hostname}:${window.location.port}${wsUrl}`;

    client.current = new Client({
      brokerURL: fullUrl,
      debug: debug
        ? function (str) {
            console.log(str);
          }
        : noop,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    client.current.activate();

    client.current.onConnect = () => {
      setIsWsActive(true);
      destinations.forEach((destination) => subscribe(destination));
    };

    client.current.onDisconnect = () => {
      setIsWsActive(false);
      subscribtions.current.forEach((subscription) =>
        unsubscribe(subscription)
      );
    };

    return () => {
      client.current?.deactivate();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const context: WebsocketContext = useMemo(
    () => ({
      isWsActive,
      client,
      subscribe,
      unsubscribe,
      message,
      setMessage,
    }),
    [isWsActive, message, subscribe, unsubscribe]
  );

  return {
    context,
  };
}
