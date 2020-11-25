import React from 'react';
import { FileRef } from 'common/common-types';
import { TableField } from 'components/table-field/table-field';
import { TableFieldColumn } from 'components/table-field/table-field-types';
import { FileTableProps } from './file-table-types';
import { FileTableToolbar } from './file-table-toolbar';
import { FileTableActionCell } from './file-table-action-cell';

export function FileTable({
  value,
  onChange,
  disabled = false,
}: FileTableProps) {
  const columns: TableFieldColumn<FileRef>[] = [
    {
      name: 'Akce',
      datakey: 'id',
      width: 100,
      CellComponent: FileTableActionCell,
    },
    {
      name: 'Název',
      datakey: 'name',
      width: 400,
    },
  ];

  return (
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
  );
}
