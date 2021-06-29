import { useEffect, useState, useRef, useContext } from 'react';
import { useIntl } from 'react-intl';
import { noop } from 'lodash';
import { unstable_batchedUpdates } from 'react-dom';
import { useCrudSource } from 'utils/crud-source-hook';
import { useEventCallback } from 'utils/event-callback-hook';
import { SnackbarVariant } from 'composite/snackbar/snackbar-types';
import { SnackbarContext } from 'composite/snackbar/snackbar-context';
import { FormHandle } from 'composite/form/form-types';
import { Report } from '../reporting-types';
import { ReportingContext } from '../reporting-context';

export function useReportData({ id }: { id: string | undefined }) {
  const intl = useIntl();
  const { showSnackbar } = useContext(SnackbarContext);
  const { url, generate } = useContext(ReportingContext);

  const [loading, setLoading] = useState<boolean>(false);
  const formRef = useRef<FormHandle<any>>(null);
  const resultRef = useRef<FormHandle<Report | null>>(null);

  const source = useCrudSource<Report>({
    url,
    handleGetError: noop,
  });

  const handleGenerate = useEventCallback(async () => {
    if (id === undefined) {
      return;
    }

    try {
      setLoading(true);

      const input = formRef.current?.getFieldValues();

      const report: Report = await generate(id, input ?? {});

      unstable_batchedUpdates(() => {
        source.reset(report);
        setLoading(false);
      });
    } catch (err) {
      setLoading(false);

      if (err.name !== 'AbortError') {
        const message = intl.formatMessage({
          id: 'EAS_REPORTING_DATA_ERROR_LOADING',
          defaultMessage: 'Chyba načtení reportu',
        });

        showSnackbar(message, SnackbarVariant.ERROR);
      }
      return undefined;
    }
  });

  useEffect(() => {
    if (id !== undefined) {
      source.reset();
      source.get(id as string);
    } else {
      source.reset();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  useEffect(() => {
    resultRef.current?.setFieldValues(source.data);
  }, [source.data]);
  return {
    source,
    generate: handleGenerate,
    loading,
    formRef,
    resultRef,
  };
}
