import React, {
  useState,
  useCallback,
  useImperativeHandle,
  useContext,
  useRef,
} from 'react';
import { ResultStatus } from './bulk-action-dialog-api';
import { BulkActionDialogHandle } from './bulk-action-dialog-types';
import { FormHandle } from 'composite/form/form-types';
import { DialogHandle } from 'components/dialog/dialog-types';
import { TableContext, TableSelectedContext } from '../table-context';
import { useEventCallback } from 'utils/event-callback-hook';
import { SnackbarContext } from 'composite/snackbar/snackbar-context';
import { SnackbarVariant } from 'composite/snackbar/snackbar-types';
import { useIntl } from 'react-intl';
import { EvidenceContext } from 'composite/evidence/evidence-context';
import { Filter } from 'common/common-types';
import { convertToApiFilters } from '../hooks/table-data-hook';
import { TableFilterWithState } from '../table-types';

export function useBulkActionDialog<FORM_DATA>({
  url,
  paramsMapper,
  ref,
  apiCall,
}: {
  url: string;
  paramsMapper: (formData?: FORM_DATA) => Record<string, any>;
  ref: React.Ref<BulkActionDialogHandle>;
  apiCall: (
    url: string,
    filters: Filter[],
    ids: string[],
    params?: Record<string, any> | undefined
  ) => Promise<ResultStatus>;
}) {
  const dialogRef = useRef<DialogHandle>(null);
  const formRef = useRef<FormHandle<FORM_DATA>>(null);

  const [submitting, setSubmitting] = useState(false);
  const {
    preFilters,
    filters,
    filtersState,
    searchQuery,
    totalCount,
  } = useContext(TableContext);
  const { selected } = useContext(TableSelectedContext);

  const filtersWithState: TableFilterWithState[] = filters.map((filter, i) => ({
    ...filter,
    ...filtersState[i],
  }));

  const apiFilters = convertToApiFilters(
    preFilters,
    filtersWithState,
    searchQuery
  );

  const applyToSelected = selected.length > 0;
  const count = applyToSelected ? selected.length : totalCount;

  const { detailRef, tableRef } = useContext(EvidenceContext);

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

      const status = await apiCall(
        url,
        apiFilters,
        selected,
        paramsMapper(formRef.current?.getFieldValues())
      );

      setSubmitting(false);

      if (status.error === undefined) {
        detailRef.current?.refreshAll();
        tableRef.current?.resetSelection();
        tableRef.current?.refresh();

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
  }, [
    apiCall,
    apiFilters,
    detailRef,
    intl,
    paramsMapper,
    selected,
    showSnackbar,
    tableRef,
    url,
  ]);

  return {
    submitting,
    dialogRef,
    formRef,
    applyToSelected,
    count,
    confirm,
  };
}
