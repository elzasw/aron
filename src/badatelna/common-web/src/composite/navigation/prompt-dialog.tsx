import React, { forwardRef } from 'react';

import Typography from '@material-ui/core/Typography';
import { PromptDialogProps } from './navigation-types';
import { Dialog } from 'components/dialog/dialog';
import { DialogHandle } from 'components/dialog/dialog-types';

export const PromptDialog = forwardRef<DialogHandle, PromptDialogProps>(
  function PromptDialog({ prompts, onConfirm, onCancel }, ref) {
    const prompt = prompts[0] ?? { title: '', text: '' };

    return (
      <Dialog
        ref={ref}
        title={prompt.title}
        onConfirm={onConfirm}
        onCancel={onCancel}
      >
        {() => <Typography>{prompt.text}</Typography>}
      </Dialog>
    );
  }
);
