import React from 'react';
import { FormattedMessage } from 'react-intl';
import { TableColumn } from 'composite/table/table-types';
import { TableCells } from 'composite/table/table-cells';
import { Translation } from './translations-types';
import { TableFilterCells } from 'composite/table/table-filter-cells';
import { useLanguages } from './translations-api';

export function useColumns(): TableColumn<Translation>[] {
  return [
    {
      datakey: 'language',
      sortkey: 'language.name',
      filterkey: 'language.id',
      name: (
        <FormattedMessage
          id="EAS_TRANSLATIONS_COLUMN_LANGUAGE"
          defaultMessage="Jazyk"
        />
      ),
      width: 200,
      CellComponent: TableCells.TextCell,
      FilterComponent: TableFilterCells.useFilterSelectCellFactory(
        useLanguages
      ),
      valueMapper: TableCells.useSelectCellFactory(useLanguages),
      sortable: true,
      filterable: true,
    },
  ];
}
