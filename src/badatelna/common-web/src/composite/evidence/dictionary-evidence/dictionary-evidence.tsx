import React, { useMemo, ComponentType } from 'react';
import { DictionaryObject } from 'common/common-types';
import { TableColumn, TableSort } from 'composite/table/table-types';
import { DetailToolbarProps } from 'composite/detail/detail-types';
import { useAuthoredEvidence } from '../authored-evidence/authored-evidence';
import { DictionaryToolbar } from './dictionary-toolbar';
import { DictionaryFields } from './dictionary-fields';
import { useColumns } from './dictionary-columns';
import { EvidenceProps } from '../evidence-types';
import { useEventCallback } from 'utils/event-callback-hook';

export function useDictionaryEvidence<OBJECT extends DictionaryObject>(
  options: EvidenceProps<OBJECT>
): EvidenceProps<OBJECT> {
  const columns: TableColumn<OBJECT>[] = useColumns({
    columns: options.tableProps?.columns,
  });

  const defaultSorts: TableSort[] = useMemo(
    () =>
      options.tableProps?.defaultSorts ?? [
        { field: 'order', datakey: 'order', order: 'ASC', type: 'FIELD' },
        { field: 'name', datakey: 'name', order: 'ASC', type: 'FIELD' },
      ],
    [options.tableProps?.defaultSorts]
  );

  const GeneralFieldsComponent: ComponentType = useMemo(
    () =>
      function Fields() {
        return (
          <DictionaryFields
            FieldsComponent={options.detailProps?.GeneralFieldsComponent}
          />
        );
      },
    [options.detailProps?.GeneralFieldsComponent]
  );

  const toolbarProps: DetailToolbarProps<OBJECT> = useMemo(
    () => ({
      after: <DictionaryToolbar />,
      ...options.detailProps?.toolbarProps,
    }),
    [options.detailProps?.toolbarProps]
  );

  const initNewItem = useEventCallback(() => {
    const obj: OBJECT = {
      active: true,
    } as any;

    if (options.detailProps?.initNewItem !== undefined) {
      return {
        ...obj,
        ...options.detailProps?.initNewItem(),
      };
    } else {
      return obj;
    }
  });

  return useAuthoredEvidence({
    ...options,
    tableProps: {
      ...options.tableProps,
      columns,

      defaultSorts,
    },
    detailProps: {
      ...options.detailProps,
      initNewItem,
      GeneralFieldsComponent,
      toolbarProps,
    },
  });
}
