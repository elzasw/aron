import { useRef } from 'react';
import { DialogHandle } from 'components/dialog/dialog-types';
import { useEventCallback } from 'utils/event-callback-hook';

export function useTableReports() {
  const reportDialogRef = useRef<DialogHandle>(null);
  const openReportDialog = useEventCallback(() =>
    reportDialogRef.current?.open()
  );
  const closeReportDialog = useEventCallback(() =>
    reportDialogRef.current?.close()
  );

  return {
    reportDialogRef,
    openReportDialog,
    closeReportDialog,
  };
}
