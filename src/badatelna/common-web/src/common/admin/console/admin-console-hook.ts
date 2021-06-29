import { useRef, useEffect, useContext, useState } from 'react';
import { IMessage } from '@stomp/stompjs';
import { FormHandle } from 'composite/form/form-types';
import { useEventCallback } from 'utils/event-callback-hook';
import {
  ExecuteScriptRequest,
  ExecuteScriptResponse,
} from './admin-console-types';
import { WebsocketContext } from 'common/web-socket/web-socket-context';

export function useAdminConsole() {
  const { client, subscribe, unsubscribe } = useContext(WebsocketContext);
  const resultForm = useRef<FormHandle>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = useEventCallback((values: ExecuteScriptRequest) => {
    setLoading(true);

    const resultSubscription = subscribe(
      '/topic/script/execute/response',
      (message) => {
        unsubscribe(resultSubscription!);
        unsubscribe(consoleSubscription!);

        handleResultIncomming(message);
        setLoading(false);
      }
    );

    const consoleSubscription = subscribe(
      '/topic/script/execute/out',
      (message) => {
        handleConsoleIncomming(message);
      }
    );

    client.current?.publish({
      destination: '/app/script/execute/request',
      body: JSON.stringify(values),
      skipContentLengthHeader: true,
    });
  });

  const handleResultIncomming = useEventCallback((message: IMessage) => {
    const result: ExecuteScriptResponse = JSON.parse(message.body);

    if (result.error != null) {
      resultForm.current?.setFieldValue('result', result.error);
    } else {
      resultForm.current?.setFieldValue(
        'result',
        JSON.stringify(result.result)
      );
    }
  });

  const handleConsoleIncomming = useEventCallback((message: IMessage) => {
    const values = resultForm.current?.getFieldValues();
    resultForm.current?.setFieldValue('console', values.console + message.body);
  });

  const handleLogIncomming = useEventCallback((message: IMessage) => {
    const values = resultForm.current?.getFieldValues();
    resultForm.current?.setFieldValue('log', values.log + message.body);
  });

  const handleClear = useEventCallback(() => {
    resultForm.current?.clearForm();
  });

  useEffect(() => {
    const subscription = subscribe('/topic/script/log', (message) =>
      handleLogIncomming(message)
    );

    return () => {
      unsubscribe(subscription!);
    };
  }, [handleLogIncomming, subscribe, unsubscribe]);

  return {
    handleSubmit,
    handleClear,
    resultForm,
    loading,
  };
}
