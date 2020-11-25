import React, {
  useState,
  useCallback,
  useImperativeHandle,
  useContext,
  useRef,
} from 'react';
import { bulkActionDialogApi } from './bulk-action-dialog-api';
import { BulkActionDialogHandle } from './bulk-action-dialog-types';
import { FormHandle } from 'composite/form/form-types';
import { DialogHandle } from 'components/dialog/dialog-types';
import { TableContext, TableSelectedContext } from '../table-context';
import { useEventCallback } from 'utils/event-callback-hook';
import { SnackbarContext } from 'composite/snackbar/snackbar-context';
import { SnackbarVariant } from 'composite/snackbar/snackbar-types';
import { useIntl } from 'react-intl';

export function useBulkActionDialog<FORM_DATA>({
  url,
  paramsMapper,
  ref,
}: {
  url: string;
  paramsMapper: (formData?: FORM_DATA) => Record<string, any>;
  ref: React.Ref<BulkActionDialogHandle>;
}) {
  const dialogRef = useRef<DialogHandle>(null);
  const formRef = useRef<FormHandle<FORM_DATA>>(null);

  const [submitting, setSubmitting] = useState(false);
  const { totalCount } = useContext(TableContext); // apiFilters
  const { selected } = useContext(TableSelectedContext);

  const applyToSelected = selected.length > 0;
  const count = applyToSelected ? selected.length : totalCount;

  // const { tableRef, formRef } = useContext(EvidenceContext);
  // const refreshTable = tableRef.current?.refresh!;
  // const refreshForm = formRef.current?.refresh!;
  const { showSnackbar } = useContext(SnackbarContext);

  const intl = useIntl();

  const openDialog = useEventCallback(() => {
    dialogRef.current?.open();
  });

  useImperativeHandle(ref, () => ({
    openDialog,
  }));

  const confirm = useCallback(() => {
    async function call() {
      setSubmitting(true);
      const status = await bulkActionDialogApi(
        url,
        [], // fixme: apiFilters
        selected,
        paramsMapper(formRef.current?.getFieldValues())
      );

      setSubmitting(false);

      if (status.error === undefined) {
        // fixme
        // refreshTable();
        // refreshForm();

        showSnackbar(
          intl.formatMessage({
            id: 'EAS_TABLE_BULK_ACTION_DIALOG_MSG_SUCCESS',
            defaultMessage: 'Hromadná akce proběhla úspěšne.',
          }),
          SnackbarVariant.SUCCESS
        );
      } else {
        showSnackbar(
          intl.formatMessage({
            id: 'EAS_TABLE_BULK_ACTION_DIALOG_MSG_ERRORs',
            defaultMessage: `Hromadná akce selhala. ${status.error}`,
          }),
          SnackbarVariant.ERROR
        );
      }
    }

    call();
  }, [intl, paramsMapper, selected, showSnackbar, url]);

  return {
    submitting,
    dialogRef,
    formRef,
    applyToSelected,
    count,
    confirm,
  };
}
