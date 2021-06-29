import React from 'react';
import { useWebsocket } from './web-socket-hook';
import { WebsocketContext } from './web-socket-context';

export function WebsocketProvider({
  wsUrl,
  destinations,
  children,
  debug = true,
}: React.PropsWithChildren<{
  wsUrl: string;
  destinations: string[];
  debug: boolean;
}>) {
  const { context } = useWebsocket({
    wsUrl,
    destinations,
    debug,
  });

  return (
    <WebsocketContext.Provider value={context}>
      {context.isWsActive && children}
    </WebsocketContext.Provider>
  );
}
