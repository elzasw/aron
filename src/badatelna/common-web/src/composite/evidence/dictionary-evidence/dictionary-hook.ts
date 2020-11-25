import { useContext, useRef } from 'react';
import { unstable_batchedUpdates } from 'react-dom';
import { useIntl } from 'react-intl';
import { useEventCallback } from 'utils/event-callback-hook';
import { CrudSource, DictionaryObject } from 'common/common-types';
import { activateItem, deactivateItem } from './dictionary-api';
import { AbortableFetch } from 'utils/abortable-fetch';
import { SnackbarContext } from 'composite/snackbar/snackbar-context';
import { SnackbarVariant } from 'composite/snackbar/snackbar-types';

export function useDictionary({
  source,
  onPersisted,
}: {
  source: CrudSource<DictionaryObject>;
  onPersisted: (id: string | null) => void;
}) {
  const intl = useIntl();
  const { showSnackbar } = useContext(SnackbarContext);
  const fetch = useRef<AbortableFetch | null>(null);

  const activate = useEventCallback(async () => {
    try {
      source.setLoading(true);
      if (fetch.current !== null) {
        fetch.current.abort();
      }

      fetch.current = activateItem(source.url, source.data!.id);

      await fetch.current.none();

      const message = intl.formatMessage({
        id: 'EAS_EVIDENCE_MSG_ACTIVATED_SUCCESS',
        defaultMessage: 'Záznam byl úspěšně aktivován.',
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
            id: 'EAS_EVIDENCE_MSG_ACTIVATED_ERROR',
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

  const deactivate = useEventCallback(async () => {
    try {
      source.setLoading(true);
      if (fetch.current !== null) {
        fetch.current.abort();
      }

      fetch.current = deactivateItem(source.url, source.data!.id);

      await fetch.current.none();

      const message = intl.formatMessage({
        id: 'EAS_EVIDENCE_MSG_DEACTIVATED_SUCCESS',
        defaultMessage: 'Záznam byl úspěšně deaktivován.',
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
            id: 'EAS_EVIDENCE_MSG_DEACTIVATED_ERROR',
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

  return { activate, deactivate };
}
