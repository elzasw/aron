import { useMemo, useState, useContext, useRef } from 'react';
import { unstable_batchedUpdates } from 'react-dom';
import { useIntl } from 'react-intl';
import {
  ReportType,
  ReportRequest,
  ReportTemplate,
  ReportRequestState,
} from 'common/common-types';
import { TableFieldColumn } from 'components/table-field/table-field-types';
import { SnackbarVariant } from 'composite/snackbar/snackbar-types';
import { SnackbarContext } from 'composite/snackbar/snackbar-context';
import { useCrudSource } from 'utils/crud-source-hook';
import { useEventCallback } from 'utils/event-callback-hook';
import { useTemplates } from './report-dialog-api';
import { sleep } from 'utils/sleep';
import { DialogHandle } from 'components/dialog/dialog-types';

export function useReportDialogHook(
  tag: string,
  provideData: () => Record<string, any>
) {
  const { showSnackbar } = useContext(SnackbarContext);
  const intl = useIntl();
  const [asyncLoading, setAsyncLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [state, setState] = useState<ReportRequestState | undefined>(undefined);
  const [resultId, setResultId] = useState<string | undefined>();

  const ref = useRef<DialogHandle>(null);
  const [selectedFormat, setSelectedFormat] = useState<ReportType>(
    ReportType.PDF
  );
  const [
    selectedTemplate,
    setSelectedTemplate,
  ] = useState<ReportTemplate | null>(null);
  const templateSource = useTemplates(tag);
  const requestSource = useCrudSource<ReportRequest>({
    url: '/api/report/requests',
    createMessages: {
      successMessage: intl.formatMessage({
        id: 'EAS_REPORT_DIALOG_MSG_SUCCESS',
        defaultMessage: 'Požadavek na tisk byl zařazen do fronty.',
      }),
      errorMessage: intl.formatMessage({
        id: 'EAS_REPORT_DIALOG_MSG_ERROR',
        defaultMessage:
          'Nastala chyba při vytváření požadavku na tisk: {detail}.',
      }),
    },
  });

  const selectTemplate = useEventCallback((template: ReportTemplate | null) => {
    setSelectedTemplate(template);
  });

  const generateSync = useEventCallback(async () => {
    unstable_batchedUpdates(() => {
      setState(undefined);
      setLoading(true);
    });

    await submitRequest();
    setState(requestSource.data?.state);

    let state: ReportRequestState | undefined;
    let data: ReportRequest | undefined;

    try {
      do {
        await sleep(2000);

        data = await requestSource.refresh();
        state = data?.state;
        setState(state);
      } while (
        state === ReportRequestState.PENDING ||
        state === ReportRequestState.PROCESSING
      );
    } finally {
      setLoading(false);
    }

    if (state === ReportRequestState.PROCESSED) {
      const message = intl.formatMessage({
        id: 'EAS_REPORT_DIALOG_MSG_SYNC_GENERATED_SUCCESS',
        defaultMessage: 'Tisk byl vygenerován. Můžete ho stáhnout.',
      });
      showSnackbar(message, SnackbarVariant.SUCCESS);

      setResultId(data?.result?.id);
    } else if (state === ReportRequestState.FAILED) {
      const message = intl.formatMessage(
        {
          id: 'EAS_REPORT_DIALOG_MSG_SYNC_GENERATED_ERROR',
          defaultMessage: 'Nastala chyba při generování tisku: {detail}.',
        },
        {
          detail: data?.message,
        }
      );
      showSnackbar(message, SnackbarVariant.ERROR);

      setResultId(undefined);
    }
  });

  const downloadSync = useEventCallback(() => {
    setState(undefined);
  });

  const generateASync = useEventCallback(async () => {
    setAsyncLoading(true);

    try {
      await submitRequest();
    } finally {
      setAsyncLoading(false);
    }

    ref.current?.close();
  });

  const submitRequest = useEventCallback(async () => {
    if (selectedTemplate === null) {
      const message = intl.formatMessage({
        id: 'EAS_REPORT_DIALOG_MSG_MISSING_TEMPLATE_ERROR',
        defaultMessage: 'Není vybraná šablona',
      });
      showSnackbar(message, SnackbarVariant.ERROR);
      return false;
    }

    if (selectedFormat === null) {
      const message = intl.formatMessage({
        id: 'EAS_REPORT_DIALOG_MSG_MISSING_FORMAT_ERROR',
        defaultMessage: 'Není vybrán formát',
      });
      showSnackbar(message, SnackbarVariant.ERROR);
      return false;
    }

    const request: ReportRequest = {
      id: '',
      template: selectedTemplate ?? undefined,
      type: selectedFormat,
      configuration: JSON.stringify(provideData()),
    };

    await requestSource.create(request);
  });

  const columns: TableFieldColumn<any>[] = useMemo(
    () => [
      {
        name: 'Název',
        datakey: 'label',
        width: 500,
        visible: true,
      },
    ],
    []
  );

  const syncLoading =
    state === ReportRequestState.PENDING ||
    state === ReportRequestState.PROCESSING;

  return {
    loading,
    state,
    ref,
    asyncLoading,
    syncLoading,
    columns,
    templates: templateSource.items,
    selectedFormat,
    resultId,
    setSelectedFormat,
    selectTemplate,
    generateSync,
    generateASync,
    downloadSync,
  };
}
