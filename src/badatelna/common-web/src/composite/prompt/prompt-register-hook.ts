import { useEffect, useContext, DependencyList } from 'react';
import { Prompt } from './prompt-types';
import { PromptContext } from './prompt-context';

export function usePrompts(prompts: Prompt[], deps: DependencyList = []) {
  const { registerPrompts, unregisterPrompts } = useContext(PromptContext);

  useEffect(() => {
    registerPrompts(prompts);

    return () => {
      unregisterPrompts(prompts);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
}
