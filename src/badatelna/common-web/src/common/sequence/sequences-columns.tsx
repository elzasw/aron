import { useIntl } from 'react-intl';
import { TableColumn } from 'composite/table/table-types';
import { TableCells } from 'composite/table/table-cells';
import { Sequence } from './sequences-types';

export function useColumns(): TableColumn<Sequence>[] {
  const intl = useIntl();
  return [
    {
      datakey: 'description',
      name: intl.formatMessage({
        id: 'EAS_SEQUENCES_COLUMN_DESCRIPTION',
        defaultMessage: 'Popis',
      }),
      width: 200,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'format',
      name: intl.formatMessage({
        id: 'EAS_SEQUENCES_COLUMN_FORMAT',
        defaultMessage: 'Formát',
      }),
      width: 200,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'counter',
      name: intl.formatMessage({
        id: 'EAS_SEQUENCES_COLUMN_COUNTER',
        defaultMessage: 'Počítadlo',
      }),
      width: 200,
      CellComponent: TableCells.NumberCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'local',
      name: intl.formatMessage({
        id: 'EAS_SEQUENCES_COLUMN_LOCAL',
        defaultMessage: 'Lokální',
      }),
      width: 100,
      CellComponent: TableCells.BooleanCell,
      sortable: true,
      filterable: true,
    },
  ];
}
