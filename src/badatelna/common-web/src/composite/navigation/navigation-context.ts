import { createContext } from 'react';

export interface NavigationPrompt {
  title: string;
  text: string;
  clearCallback?: () => void;
}

export interface StateAction {
  action: string;
  data: any;
}

export interface NavigationContext {
  prompts: NavigationPrompt[];
  navigate: (url: string, replace?: boolean, state?: StateAction) => void;

  testPrompts: (callback: () => void) => void;

  registerPrompt: (promt: NavigationPrompt) => void;
  unregisterPrompt: (promt: NavigationPrompt) => void;

  stateAction: StateAction | null;
}

export const NavigationContext = createContext<NavigationContext>(
  undefined as any
);
