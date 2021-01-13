import React, { useMemo } from 'react';
import { useIntl } from 'react-intl';
import { FileRef } from 'common/common-types';
import { TableField } from 'components/table-field/table-field';
import { TableFieldColumn } from 'components/table-field/table-field-types';
import { FileTableProps } from './file-table-types';
import { FileTableToolbar } from './file-table-toolbar';
import { FileTableActionCell } from './file-table-action-cell';
import { FileTableContext } from './file-table-context';

export function FileTable({
  value,
  onChange,
  disabled = false,
  maxItems,
}: FileTableProps) {
  const intl = useIntl();

  const columns: TableFieldColumn<FileRef>[] = [
    {
      name: intl.formatMessage({
        id: 'EAS_FILE_TABLE_COLUMN_ACTIONS',
        defaultMessage: 'Akce',
      }),
      datakey: 'id',
      width: 100,
      CellComponent: FileTableActionCell,
    },
    {
      name: intl.formatMessage({
        id: 'EAS_FILE_TABLE_COLUMN_NAME',
        defaultMessage: 'Název',
      }),
      datakey: 'name',
      width: 400,
    },
  ];

  const context: FileTableContext = useMemo(() => ({ maxItems }), [maxItems]);

  return (
    <FileTableContext.Provider value={context}>
      <TableField
        maxRows={4}
        showRadioCond={() => false}
        showDetailBtnCond={() => false}
        ToolbarComponent={FileTableToolbar}
        value={value}
        columns={columns}
        onChange={onChange}
        disabled={disabled}
      />
    </FileTableContext.Provider>
  );
}
