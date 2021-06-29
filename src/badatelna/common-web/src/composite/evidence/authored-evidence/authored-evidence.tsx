import React, { useMemo, ComponentType } from 'react';
import { AuthoredObject } from 'common/common-types';
import { TableColumn } from 'composite/table/table-types';
import { useDatedEvidence } from './../dated-evidence/dated-evidence';
import { EvidenceProps } from './../evidence-types';
import { AuthoredFields } from './../authored-evidence/authored-fields';
import { useColumns } from './../authored-evidence/authored-columns';

export function useAuthoredEvidence<OBJECT extends AuthoredObject>(
  options: EvidenceProps<OBJECT>
): EvidenceProps<OBJECT> {
  const columns: TableColumn<OBJECT>[] = useColumns({
    columns: options.tableProps?.columns,
  });

  const GeneralFieldsComponent: ComponentType = useMemo(
    () =>
      function Fields() {
        return (
          <AuthoredFields
            FieldsComponent={options.detailProps?.GeneralFieldsComponent}
          />
        );
      },
    [options.detailProps?.GeneralFieldsComponent]
  );

  return useDatedEvidence({
    ...options,
    tableProps: {
      ...options.tableProps,
      columns,
    },
    detailProps: {
      GeneralFieldsComponent,
      ...options.detailProps,
    },
  });
}
