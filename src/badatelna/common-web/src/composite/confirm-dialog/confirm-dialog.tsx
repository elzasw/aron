import React, { forwardRef } from 'react';

import Typography from '@material-ui/core/Typography';
import { Dialog } from 'components/dialog/dialog';
import { DialogHandle } from 'components/dialog/dialog-types';
import { ConfirmDialogProps } from './confirm-dialog-types';

export const ConfirmDialog = forwardRef<DialogHandle, ConfirmDialogProps>(
  function ConfirmDialog({ title, text, onConfirm, onCancel }, ref) {
    return (
      <Dialog ref={ref} title={title} onConfirm={onConfirm} onCancel={onCancel}>
        {() => <Typography>{text}</Typography>}
      </Dialog>
    );
  }
);
