import { ComponentType, RefAttributes } from 'react';
import { DialogHandle } from 'components/dialog/dialog-types';
import { Prompt } from './navigation-context';

export interface PromptDialogProps {
  prompts: Prompt[];
  onConfirm: () => void;
  onCancel: () => void;
}

export interface NavigationProviderProps {
  PromptDialogComponent?: ComponentType<
    PromptDialogProps & RefAttributes<DialogHandle>
  >;
}
