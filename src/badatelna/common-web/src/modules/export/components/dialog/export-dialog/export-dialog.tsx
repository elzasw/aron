import React, { forwardRef } from 'react';
import { DialogHandle } from 'components/dialog/dialog-types';
import { ExportDialogProps } from './export-dialog-types';
import { ExportDialogContent } from './export-dialog-content';

export const ExportDialog = forwardRef<DialogHandle, ExportDialogProps>(
  function ExportDialog(props, dialogRef) {
    return <ExportDialogContent {...props} ref={dialogRef} />;
  }
);
