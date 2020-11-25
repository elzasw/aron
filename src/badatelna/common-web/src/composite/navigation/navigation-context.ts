import { createContext } from 'react';

export interface Prompt {
  title: string;
  text: string;
}

export interface StateAction {
  action: string;
  data: any;
}

export interface NavigationContext {
  navigate: (url: string, replace?: boolean, state?: StateAction) => void;

  testPrompts: (callback: () => void) => void;

  registerPrompt: (promt: Prompt) => void;
  unregisterPrompt: (promt: Prompt) => void;

  stateAction: StateAction | null;
}

export const NavigationContext = createContext<NavigationContext>(
  undefined as any
);
