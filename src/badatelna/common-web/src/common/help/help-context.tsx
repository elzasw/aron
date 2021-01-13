import { createContext } from 'react';

export interface HelpContext {
  formContextHelpType: 'CLICKABLE' | 'HOVER';
}

export const HelpContext = createContext<HelpContext>({
  formContextHelpType: 'CLICKABLE',
});
