import { DateTimeField } from 'components/date-time-field/date-time-field';
import { filterIntervalCellFactory } from './filter-interval-cell-factory';

export const FilterDateTimeCell = filterIntervalCellFactory(DateTimeField);
