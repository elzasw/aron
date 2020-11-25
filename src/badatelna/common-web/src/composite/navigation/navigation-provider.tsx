import React, { PropsWithChildren } from 'react';
import { NavigationContext } from './navigation-context';
import { useNavigation } from './navigation-hook';

import { NavigationProviderProps } from './navigation-types';
import { PromptDialog } from './prompt-dialog';

/**
 * Main component for navigation providing context and rendering Prompt if necessary.
 *
 */
export function NavigationProvider({
  PromptDialogComponent = PromptDialog,
  children,
}: PropsWithChildren<NavigationProviderProps>) {
  const {
    context,
    dialogRef,
    prompts,
    promptConfirm,
    promptCancel,
  } = useNavigation();

  return (
    <NavigationContext.Provider value={context}>
      {children}
      <PromptDialogComponent
        ref={dialogRef}
        prompts={prompts}
        onConfirm={promptConfirm}
        onCancel={promptCancel}
      />
    </NavigationContext.Provider>
  );
}
