import { TextCell } from './cells/text-cell';
import { BooleanCell } from './cells/boolean-cell';
import { FileCell } from './cells/file-cell';
import { NumberCell } from './cells/number-cell';
import { DateCell } from './cells/date-cell';
import { DateTimeCell } from './cells/date-time-cell';
import { TimeCell } from './cells/time-cell';
import { useSelectCellFactory } from './cells/select-cell';
import { useAutocompleteCellFactory } from './cells/autocomplete-cell';

export const TableFieldCells = {
  BooleanCell,
  TextCell,
  NumberCell,
  DateCell,
  DateTimeCell,
  TimeCell,
  useSelectCellFactory,
  useAutocompleteCellFactory,
  FileCell,
};
