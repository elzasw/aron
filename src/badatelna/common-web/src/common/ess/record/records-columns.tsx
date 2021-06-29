import { useIntl } from 'react-intl';
import { TableColumn } from 'composite/table/table-types';
import { TableCells } from 'composite/table/table-cells';
import { Record } from '../ess-types';

/**
 * fixme: add iniciacni dokument
 */
export function useColumns(): TableColumn<Record>[] {
  const intl = useIntl();
  return [
    {
      datakey: 'recordSymbol',
      name: intl.formatMessage({
        id: 'ESS_RECORDS_COLUMN_RECORD_SYMBOL',
        defaultMessage: 'Spisová značka',
      }),
      width: 150,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'name',
      name: intl.formatMessage({
        id: 'ESS_RECORDS_COLUMN_NAME',
        defaultMessage: 'Název',
      }),
      width: 200,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'description',
      name: intl.formatMessage({
        id: 'ESS_RECORDS_COLUMN_DESCRIPTION',
        defaultMessage: 'Popis',
      }),
      width: 200,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'classificationCode',
      name: intl.formatMessage({
        id: 'ESS_RECORDS_COLUMN_CLASSIFICATION_CODE',
        defaultMessage: 'Spisový znak',
      }),
      width: 120,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'barCode',
      name: intl.formatMessage({
        id: 'ESS_RECORDS_COLUMN_BARCODE',
        defaultMessage: 'Čárový kód',
      }),
      width: 120,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
  ];
}
