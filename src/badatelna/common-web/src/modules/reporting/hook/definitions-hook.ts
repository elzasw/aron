import { useState, useEffect, useContext } from 'react';
import { useIntl } from 'react-intl';
import { unstable_batchedUpdates } from 'react-dom';
import { useEventCallback } from 'utils/event-callback-hook';
import { SnackbarContext } from 'composite/snackbar/snackbar-context';
import { SnackbarVariant } from 'composite/snackbar/snackbar-types';
import { ReportDefinition } from '../reporting-types';
import { ReportingContext } from '../reporting-context';

export function useReportDefinitionsLogic() {
  const intl = useIntl();
  const { loadDefinitions } = useContext(ReportingContext);
  const { showSnackbar } = useContext(SnackbarContext);

  const [loading, setLoading] = useState(false);
  const [definitions, setDefinitions] = useState<ReportDefinition[]>([]);

  const load = useEventCallback(async () => {
    try {
      setLoading(true);

      const definitions: ReportDefinition[] = await loadDefinitions();

      unstable_batchedUpdates(() => {
        setDefinitions(definitions);
        setLoading(false);
      });
    } catch (err) {
      setLoading(false);

      if (err.name !== 'AbortError') {
        const message = intl.formatMessage({
          id: 'EAS_REPORTING_DEFINITIONS_ERROR_LOADING',
          defaultMessage: 'Chyba načtení reportů',
        });

        showSnackbar(message, SnackbarVariant.ERROR);
      }
      return undefined;
    }
  });

  useEffect(() => {
    load();
  }, [load]);

  return {
    definitions,
    loading,
  };
}
