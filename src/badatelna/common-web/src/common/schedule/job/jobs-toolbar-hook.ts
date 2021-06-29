import { useRef, useContext } from 'react';
import { unstable_batchedUpdates } from 'react-dom';
import { useIntl } from 'react-intl';
import { startCall, cancelCall } from './jobs-api';
import { CrudSource } from 'common/common-types';
import { SnackbarContext } from 'composite/snackbar/snackbar-context';
import { AbortableFetch } from 'utils/abortable-fetch';
import { DialogHandle } from 'components/dialog/dialog-types';
import { useEventCallback } from 'utils/event-callback-hook';
import { SnackbarVariant } from 'composite/snackbar/snackbar-types';
import { Job } from '../schedule-types';

export function useScheduleJobsToolbar({
  source,
  onPersisted,
}: {
  source: CrudSource<Job>;
  onPersisted: (id: string | null) => void;
}) {
  const intl = useIntl();
  const { showSnackbar } = useContext(SnackbarContext);
  const fetch = useRef<AbortableFetch | null>(null);
  const startDialogRef = useRef<DialogHandle>(null);
  const cancelDialogRef = useRef<DialogHandle>(null);

  const openStartDialog = useEventCallback(() => {
    startDialogRef.current?.open();
  });

  const openCancelDialog = useEventCallback(() => {
    cancelDialogRef.current?.open();
  });

  const start = useEventCallback(async () => {
    try {
      source.setLoading(true);
      if (fetch.current !== null) {
        fetch.current.abort();
      }

      fetch.current = startCall(source.url, source.data!.id);

      await fetch.current.none();

      const message = intl.formatMessage({
        id: 'EAS_SCHEDULE_JOBS_MSG_START_SUCCESS',
        defaultMessage: 'Úloha byla úspěšně spuštěna.',
      });

      unstable_batchedUpdates(() => {
        showSnackbar(message, SnackbarVariant.SUCCESS);
        source.setLoading(false);
      });

      onPersisted(source.data!.id);
      await source.refresh();
    } catch (err) {
      source.setLoading(false);

      if (err.name !== 'AbortError') {
        const message = intl.formatMessage(
          {
            id: 'EAS_SCHEDULE_JOBS_MSG_START_ERROR',
            defaultMessage: 'Chyba volání funkce: {detail}',
          },
          { detail: err.message }
        );

        showSnackbar(message, SnackbarVariant.ERROR);

        throw err;
      }
      return undefined;
    }
  });

  const cancel = useEventCallback(async () => {
    try {
      source.setLoading(true);
      if (fetch.current !== null) {
        fetch.current.abort();
      }

      fetch.current = cancelCall(source.url, source.data!.id);

      await fetch.current.none();

      const message = intl.formatMessage({
        id: 'EAS_SCHEDULE_JOBS_MSG_CANCEL_SUCCESS',
        defaultMessage: 'Úloha byla úspěšně zrušena.',
      });

      unstable_batchedUpdates(() => {
        showSnackbar(message, SnackbarVariant.SUCCESS);
        source.setLoading(false);
      });

      onPersisted(source.data!.id);
      await source.refresh();
    } catch (err) {
      source.setLoading(false);

      if (err.name !== 'AbortError') {
        const message = intl.formatMessage(
          {
            id: 'EAS_SCHEDULE_JOBS_MSG_CANCEL_ERROR',
            defaultMessage: 'Chyba volání funkce: {detail}',
          },
          { detail: err.message }
        );

        showSnackbar(message, SnackbarVariant.ERROR);

        throw err;
      }
      return undefined;
    }
  });

  return {
    startDialogRef,
    cancelDialogRef,
    openStartDialog,
    openCancelDialog,
    start,
    cancel,
  };
}
