import { useMemo, useState, useRef } from 'react';
import { useHistory, useLocation } from 'react-router-dom';
import { useEventCallback } from 'utils/event-callback-hook';
import {
  NavigationContext,
  NavigationPrompt,
  StateAction,
} from './navigation-context';
import { DialogHandle } from 'components/dialog/dialog-types';

type Callback = () => void;

export function useNavigation() {
  const history = useHistory();
  const { state } = useLocation();

  const [prompts, setPrompts] = useState<NavigationPrompt[]>([]);
  const dialogRef = useRef<DialogHandle>(null);
  const callbackRef = useRef<Callback>();

  const navigateInternal = useEventCallback(
    (url: string, replace: boolean, state: StateAction | undefined) => {
      if (replace) {
        history.replace(url, state);
      } else {
        history.push(url, state);
      }
    }
  );

  const testPrompts = useEventCallback((callback: Callback) => {
    if (prompts.length > 0) {
      callbackRef.current = callback;
      dialogRef.current?.open();
    } else {
      callback();
    }
  });

  const navigate = useEventCallback(
    (
      url: string,
      replace = false,
      state: StateAction | undefined = undefined
    ) => {
      testPrompts(() => {
        navigateInternal(url, replace, state);
      });
    }
  );
  const registerPrompt = useEventCallback((prompt: NavigationPrompt) => {
    if (!prompts.includes(prompt)) {
      setPrompts([...prompts, prompt]);
    }
  });
  const unregisterPrompt = useEventCallback((prompt: NavigationPrompt) => {
    if (prompts.includes(prompt)) {
      setPrompts(prompts.filter((p) => p !== prompt));
    }
  });

  const promptConfirm = useEventCallback(() => {
    const callback = callbackRef.current!;
    callbackRef.current = undefined;

    prompts.forEach(
      (prompt) => prompt.clearCallback !== undefined && prompt.clearCallback()
    );
    setPrompts([]);
    callback();
  });

  const promptCancel = useEventCallback(() => {
    callbackRef.current = undefined;
  });

  const context: NavigationContext = useMemo(
    () => ({
      prompts,
      navigate,
      testPrompts,
      registerPrompt,
      unregisterPrompt,
      stateAction: state != null ? (state as StateAction) : null,
    }),
    [prompts, navigate, testPrompts, registerPrompt, unregisterPrompt, state]
  );

  return { context, dialogRef, prompts, promptConfirm, promptCancel };
}
