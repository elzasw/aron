import { useMemo } from 'react';
import { useIntl } from 'react-intl';
import { TableFieldColumn } from 'components/table-field/table-field-types';
import { TableFieldCells } from 'components/table-field/table-field-cells';
import { TableColumn } from 'composite/table/table-types';
import { TableCells } from 'composite/table/table-cells';
import { TableFilterCells } from 'composite/table/table-filter-cells';
import { SignRequest, SignContent } from '../signing-types';
import { useRequestStates } from '../signing-api';

/**
 * fixme: add record and components, type
 */
export function useColumns(): TableColumn<SignRequest>[] {
  const intl = useIntl();
  return [
    {
      datakey: 'name',
      name: intl.formatMessage({
        id: 'EAS_SIGNING_REQUESTS_COLUMN_NAME',
        defaultMessage: 'Název',
      }),
      width: 200,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'user',
      displaykey: 'user.name',
      sortkey: 'user.name',
      filterkey: 'user.name',
      name: intl.formatMessage({
        id: 'EAS_SIGNING_REQUESTS_COLUMN_USER',
        defaultMessage: 'Uživatel',
      }),
      width: 150,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'state',
      sortkey: 'state.name',
      filterkey: 'state.id',
      name: intl.formatMessage({
        id: 'EAS_SIGNING_REQUESTS_COLUMN_STATE',
        defaultMessage: 'Stav',
      }),
      width: 150,
      CellComponent: TableCells.TextCell,
      FilterComponent: TableFilterCells.useFilterSelectCellFactory(
        useRequestStates
      ),
      valueMapper: TableCells.useSelectCellFactory(useRequestStates),
      sortable: true,
      filterable: true,
    },

    {
      datakey: 'error',
      name: intl.formatMessage({
        id: 'EAS_SIGNING_REQUESTS_COLUMN_ERROR',
        defaultMessage: 'Chyba',
      }),
      width: 200,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
  ];
}

export function useContentColumns(): TableFieldColumn<SignContent>[] {
  const intl = useIntl();

  return useMemo(
    () => [
      {
        datakey: 'toSign',
        name: intl.formatMessage({
          id: 'EAS_SIGNING_CONTENTS__COLUMN_TO_SIGN',
          defaultMessage: 'K podepsání',
        }),
        width: 250,
        CellComponent: TableFieldCells.FileCell,
      },
      {
        datakey: 'signed',
        name: intl.formatMessage({
          id: 'EAS_SIGNING_CONTENTS__COLUMN_SIGNED',
          defaultMessage: 'Podepsáno',
        }),
        width: 250,
        CellComponent: TableFieldCells.FileCell,
      },
    ],
    [intl]
  );
}
