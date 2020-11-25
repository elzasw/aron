import { TextCell } from './cells/text-cell';
import { NumberCell } from './cells/number-cell';
import { DateCell } from './cells/date-cell';
import { DateTimeCell } from './cells/date-time-cell';
import { TimeCell } from './cells/time-cell';
import { BooleanCell } from './cells/boolean-cell';
import {
  dictionaryColumnMapper,
  dictionaryArrayColumnMapper,
} from './cells/dictionary-column-mapper';
import { useSelectCellFactory } from './cells/select-column-mapper';

export const TableCells = {
  BooleanCell,
  TextCell,
  NumberCell,
  DateCell,
  DateTimeCell,
  TimeCell,
  useSelectCellFactory,
  dictionaryColumnMapper,
  dictionaryArrayColumnMapper,
};
