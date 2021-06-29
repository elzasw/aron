import { createContext } from 'react';
import { PromptContextType } from './prompt-types';

export const PromptContext = createContext<PromptContextType>(undefined as any);
