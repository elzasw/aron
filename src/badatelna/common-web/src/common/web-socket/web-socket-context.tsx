import { createContext, RefObject } from 'react';
import { Client, StompSubscription, messageCallbackType } from '@stomp/stompjs';

export interface WebsocketContext {
  /**
   * True if WS connection has been successfully establsihed.
   */
  isWsActive: boolean;

  /**
   * Subscribe method. If `callback` is not provided and message will be received from server, it will be set into `message` state.
   *
   * Subsctibe without `callback` can be user in combination with useWebsocketEffect.
   */
  subscribe: (
    destination: string,
    callback?: messageCallbackType
  ) => StompSubscription | undefined;

  /**
   * Unsubscibe method.
   */
  unsubscribe: (subscription: StompSubscription) => void;

  /**
   * Received message from server. Accessible from subscribtion, where no callback is provided (see WebsocketContext.subscribe).
   */
  message: string;

  /**
   * Message setter.
   */
  setMessage: (message: string) => void;

  /**
   * Websocket client to execute custom action.
   */
  client: RefObject<Client | undefined>;
}

export const WebsocketContext = createContext<WebsocketContext>(null as any);
