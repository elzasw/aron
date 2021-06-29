import { ComponentType, RefAttributes } from 'react';
import { DialogHandle } from 'components/dialog/dialog-types';
import { NavigationPrompt } from './navigation-context';

export interface PromptDialogProps {
  prompts: NavigationPrompt[];
  onConfirm: () => void;
  onCancel: () => void;
}

export interface NavigationProviderProps {
  PromptDialogComponent?: ComponentType<
    PromptDialogProps & RefAttributes<DialogHandle>
  >;
}
