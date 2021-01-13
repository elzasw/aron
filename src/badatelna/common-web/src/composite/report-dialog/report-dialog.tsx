import React, { forwardRef, useContext } from 'react';
import { DialogHandle } from 'components/dialog/dialog-types';
import { ReportDialogProps } from './report-dialog-types';
import { ReportDialogContent } from './report-dialog-content';
import { ReportContext } from 'common/report/report-context';
import { FilesProvider } from 'common/files/files-provider';

export const ReportDialog = forwardRef<DialogHandle, ReportDialogProps>(
  function ReportDialog(props, dialogRef) {
    const { url } = useContext(ReportContext);

    return (
      <FilesProvider url={url}>
        <ReportDialogContent {...props} ref={dialogRef} />
      </FilesProvider>
    );
  }
);
