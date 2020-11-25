import React from 'react';
import { FormattedMessage } from 'react-intl';
import { TableColumn } from 'composite/table/table-types';
import { TableCells } from 'composite/table/table-cells';
import { Sequence } from './sequences-types';

export function useColumns(): TableColumn<Sequence>[] {
  return [
    {
      datakey: 'description',
      name: (
        <FormattedMessage
          id="EAS_SEQUENCES_COLUMN_DESCRIPTION"
          defaultMessage="Popis"
        />
      ),
      width: 200,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'format',
      name: (
        <FormattedMessage
          id="EAS_SEQUENCES_COLUMN_FORMAT"
          defaultMessage="Formát"
        />
      ),
      width: 200,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'counter',
      name: (
        <FormattedMessage
          id="EAS_SEQUENCES_COLUMN_COUNTER"
          defaultMessage="Počítadlo"
        />
      ),
      width: 200,
      CellComponent: TableCells.NumberCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'local',
      name: (
        <FormattedMessage
          id="EAS_SEQUENCES_COLUMN_LOCAL"
          defaultMessage="Lokální"
        />
      ),
      width: 100,
      CellComponent: TableCells.BooleanCell,
      sortable: true,
      filterable: true,
    },
  ];
}
