import { useMemo } from 'react';
import { useIntl } from 'react-intl';
import { DictionaryAutocomplete } from 'common/common-types';
import { TableFieldColumn } from 'components/table-field/table-field-types';
import { TableFieldCells } from 'components/table-field/table-field-cells';
import { TableColumn } from 'composite/table/table-types';
import { TableCells } from 'composite/table/table-cells';
import { Document, Component } from '../ess-types';
import { useComponentTypes } from '../ess-api';

/**
 * fixme: add record and components, type
 */
export function useColumns(): TableColumn<Document>[] {
  const intl = useIntl();
  return [
    {
      datakey: 'referenceNumber',
      name: intl.formatMessage({
        id: 'ESS_DOCUMENTS_COLUMN_REFERENCE_NUMBER',
        defaultMessage: 'Číslo jednací',
      }),
      width: 150,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'name',
      name: intl.formatMessage({
        id: 'ESS_DOCUMENTS_COLUMN_NAME',
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
        id: 'ESS_DOCUMENTS_COLUMN_DESCRIPTION',
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
        id: 'ESS_DOCUMENTS_COLUMN_CLASSIFICATION_CODE',
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
        id: 'ESS_DOCUMENTS_COLUMN_BARCODE',
        defaultMessage: 'Čárový kód',
      }),
      width: 120,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
  ];
}

export function useComponentColumns(): TableFieldColumn<Component>[] {
  const intl = useIntl();

  const TypeCell = TableFieldCells.useSelectCellFactory<
    Component,
    DictionaryAutocomplete
  >(useComponentTypes);

  return useMemo(
    () => [
      {
        datakey: 'file',
        name: intl.formatMessage({
          id: 'ESS_COMPONENTS_COLUMN_FILE',
          defaultMessage: 'Soubor',
        }),
        width: 200,
        CellComponent: TableFieldCells.FileCell,
      },
      {
        datakey: 'syncTime',
        name: intl.formatMessage({
          id: 'ESS_COMPONENTS_COLUMN_SYNC_TIME',
          defaultMessage: 'Čas synchronizace',
        }),
        width: 200,
        CellComponent: TableFieldCells.DateTimeCell,
      },
      {
        datakey: 'type',
        name: intl.formatMessage({
          id: 'ESS_COMPONENTS_COLUMN_TYPE',
          defaultMessage: 'Typ',
        }),
        width: 150,
        CellComponent: TypeCell,
      },
    ],
    [TypeCell, intl]
  );
}
