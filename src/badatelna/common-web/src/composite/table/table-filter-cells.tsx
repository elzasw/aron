import { FilterTextCell } from './filter-cells/filter-text-cell';
import { FilterNumberCell } from './filter-cells/filter-number-cell';
import { FilterDateCell } from './filter-cells/filter-date-cell';
import { FilterDateTimeCell } from './filter-cells/filter-date-time-cell';
import { FilterTimeCell } from './filter-cells/filter-time-cell';
import { FilterBooleanCell } from './filter-cells/filter-boolean-cell';
import { useFilterSelectCellFactory } from './filter-cells/filter-select-cell';
import { useFilterAutocompleteCellFactory } from './filter-cells/filter-autocomplete-cell';

export const TableFilterCells = {
  FilterBooleanCell,
  FilterTextCell,
  FilterNumberCell,
  FilterDateCell,
  FilterDateTimeCell,
  FilterTimeCell,
  useFilterSelectCellFactory,
  useFilterAutocompleteCellFactory,
};
