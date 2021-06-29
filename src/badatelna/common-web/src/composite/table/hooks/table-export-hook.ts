import { useRef } from 'react';
import { DialogHandle } from 'components/dialog/dialog-types';
import { useEventCallback } from 'utils/event-callback-hook';

export function useTableExports() {
  const exportDialogRef = useRef<DialogHandle>(null);
  const openExportDialog = useEventCallback(() =>
    exportDialogRef.current?.open()
  );
  const closeExportDialog = useEventCallback(() =>
    exportDialogRef.current?.close()
  );

  return {
    exportDialogRef,
    openExportDialog,
    closeExportDialog,
  };
}
