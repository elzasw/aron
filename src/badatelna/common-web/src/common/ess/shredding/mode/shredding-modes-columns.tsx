import { useIntl } from 'react-intl';
import { TableColumn } from 'composite/table/table-types';
import { TableCells } from 'composite/table/table-cells';
import { ShreddingMode } from 'common/ess/ess-types';

export function useColumns(): TableColumn<ShreddingMode>[] {
  const intl = useIntl();
  return [
    {
      datakey: 'symbol',
      name: intl.formatMessage({
        id: 'ESS_SHREDDING_MODES_COLUMN_SYMBOL',
        defaultMessage: 'Skartační znak',
      }),
      width: 150,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'period',
      name: intl.formatMessage({
        id: 'ESS_SHREDDING_MODES_COLUMN_PERIOD',
        defaultMessage: 'Skartační lhůta',
      }),
      width: 150,
      CellComponent: TableCells.NumberCell,
      sortable: true,
      filterable: true,
    },
  ];
}
