import React, { useContext, useEffect } from 'react';
import { useIntl } from 'react-intl';
import { TableField } from 'components/table-field/table-field';
import { TableFieldColumn } from 'components/table-field/table-field-types';
import { HistoryProps, History } from './history-types';
import { noop } from 'lodash';
import { useHistorySource } from './history-api';
import { TableFieldCells } from 'components/table-field/table-field-cells';
import { DetailContext } from 'composite/detail/detail-context';

export function HistoryTable({ id }: HistoryProps) {
  const intl = useIntl();

  const source = useHistorySource(id);

  const { addRefreshListener, removeRefreshListener } = useContext(
    DetailContext
  );

  useEffect(() => {
    addRefreshListener(source.reset);

    return () => {
      removeRefreshListener(source.reset);
    };
  });

  const columns: TableFieldColumn<History>[] = [
    {
      name: intl.formatMessage({
        id: 'EAS_HISTORY_COLUMN_CREATED',
        defaultMessage: 'Datum',
      }),
      datakey: 'created',
      CellComponent: TableFieldCells.DateTimeCell,
      width: 150,
    },
    {
      name: intl.formatMessage({
        id: 'EAS_HISTORY_COLUMN_OPERATION',
        defaultMessage: 'Typ změny',
      }),
      datakey: 'operation.name',
      width: 200,
    },

    {
      name: intl.formatMessage({
        id: 'EAS_HISTORY_COLUMN_CREATED_BY',
        defaultMessage: 'Autor změny',
      }),
      datakey: 'createdBy.name',
      width: 200,
    },
    {
      name: intl.formatMessage({
        id: 'EAS_HISTORY_COLUMN_DESCRIPTION',
        defaultMessage: 'Poznámka',
      }),
      datakey: 'description',
      width: 300,
    },
  ];

  return (
    <TableField
      maxRows={8}
      showRadioCond={() => false}
      showDetailBtnCond={() => false}
      showToolbar={false}
      value={source.items}
      columns={columns}
      onChange={noop}
      disabled={true}
    />
  );
}
