import { useMemo, useState, useRef } from 'react';
import { PromptContextType, Prompt, Callback } from './prompt-types';
import { DialogHandle } from 'components/dialog/dialog-types';
import { useEventCallback } from 'utils/event-callback-hook';
import { FormHandle } from 'composite/form/form-types';

export function usePrompt() {
  const [prompts, setPrompts] = useState<Prompt[]>([]);

  const [loading, setLoading] = useState(false);

  // left here to test if unregister works correctly
  // console.log(prompts);

  const [prompt, setPrompt] = useState<Prompt>();

  const dialogRef = useRef<DialogHandle>(null);
  const callbackRef = useRef<Callback>();

  const testPrompt = useEventCallback(
    ({ key, callback }: { key: string; callback: Callback }) => {
      const prompt = prompts.find((p) => p.key === key);

      if (prompt) {
        setPrompt(prompt);
        callbackRef.current = callback;
        dialogRef.current?.open();
      } else {
        callback(undefined);
      }
    }
  );

  const registerPrompts = useEventCallback((prompts: Prompt[]) => {
    setPrompts((current) => [...current, ...prompts]);
  });

  const unregisterPrompts = useEventCallback((prompts: Prompt[]) => {
    setPrompts((current) => current.filter((p) => !prompts.includes(p)));

    dialogRef.current?.close();
  });

  const promptConfirm = useEventCallback(
    async (values: any, formRef?: React.RefObject<FormHandle<any>>) => {
      try {
        setLoading(true);

        const callback = callbackRef.current!;

        let result = callback(values, formRef);

        if (result instanceof Promise) {
          result = await result;
        }

        if (result !== true) {
          callbackRef.current = undefined;

          prompts.forEach(
            (prompt) =>
              prompt.clearCallback !== undefined && prompt.clearCallback()
          );
        }

        setLoading(false);

        return result;
      } catch (err) {
        console.log(err);

        setLoading(false);
      }
    }
  );

  const promptCancel = useEventCallback(() => {
    setLoading(false);
    callbackRef.current = undefined;
  });

  const context: PromptContextType = useMemo(
    () => ({
      testPrompt,
      registerPrompts,
      unregisterPrompts,
    }),
    [testPrompt, registerPrompts, unregisterPrompts]
  );

  return { context, dialogRef, prompt, promptConfirm, promptCancel, loading };
}
