import { useIntl } from 'react-intl';
import { TableColumn } from 'composite/table/table-types';
import { TableCells } from 'composite/table/table-cells';
import { SoapMessage } from './messages-types';

export function useColumns(): TableColumn<SoapMessage>[] {
  const intl = useIntl();
  return [
    {
      datakey: 'service',
      name: intl.formatMessage({
        id: 'EAS_SOAP_LOGGER_MESSAGES_COLUMN_SERVICE',
        defaultMessage: 'Služba',
      }),
      width: 150,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
  ];
}
