import React, { useMemo, ComponentType } from 'react';
import { v4 as uuidv4 } from 'uuid';
import { useEventCallback } from 'utils/event-callback-hook';
import { DatedObject } from 'common/common-types';
import { TableColumn, TableSort } from 'composite/table/table-types';
import { EvidenceProps } from '../evidence-types';
import { useColumns } from './dated-columns';
import { DatedFields } from './dated-fields';

export function useDatedEvidence<OBJECT extends DatedObject>(
  options: EvidenceProps<OBJECT>
): EvidenceProps<OBJECT> {
  const columns: TableColumn<OBJECT>[] = useColumns({
    columns: options.tableProps?.columns,
  });

  const defaultSorts: TableSort[] = useMemo(
    () =>
      options.tableProps?.defaultSorts ?? [
        { field: 'created', datakey: 'created', order: 'DESC', type: 'FIELD' },
      ],
    [options.tableProps?.defaultSorts]
  );

  const GeneralFieldsComponent: ComponentType = useMemo(
    () =>
      function Fields() {
        return (
          <DatedFields
            FieldsComponent={options.detailProps?.GeneralFieldsComponent}
          />
        );
      },
    [options.detailProps?.GeneralFieldsComponent]
  );

  const initNewItem = useEventCallback(() => {
    const obj: OBJECT = {
      id: uuidv4(),
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

  return {
    ...options,
    tableProps: { ...options.tableProps, columns, defaultSorts },
    detailProps: {
      ...options.detailProps,
      initNewItem,
      GeneralFieldsComponent,
      toolbarProps: options.detailProps?.toolbarProps,
    },
  };
}
