import React, { PropsWithChildren } from 'react';
import { usePrompt } from './prompt-hook';
import { PromptProviderProps } from './prompt-types';
import { PromptContext } from './prompt-context';
import { PromptDialog } from './prompt-dialog';

export function PromptProvider({
  PromptDialogComponent = PromptDialog,
  children,
}: PropsWithChildren<PromptProviderProps>) {
  const {
    context,
    dialogRef,
    prompt,
    promptConfirm,
    promptCancel,
    loading,
  } = usePrompt();

  return (
    <PromptContext.Provider value={context}>
      {children}
      <PromptDialogComponent
        ref={dialogRef}
        prompt={prompt}
        onConfirm={promptConfirm}
        onCancel={promptCancel}
        loading={loading}
      />
    </PromptContext.Provider>
  );
}
