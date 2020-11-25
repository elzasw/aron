import React, { useState, useEffect, useContext } from 'react';
import DateFnsUtils from '@date-io/date-fns';
import { formatISO, isValid } from 'date-fns';
import { LocaleContext } from 'common/locale/locale-context';
import MuiPickersUtilsProvider from '@material-ui/pickers/MuiPickersUtilsProvider';
import { KeyboardDatePicker } from '@material-ui/pickers/DatePicker';
import { useEventCallback } from 'utils/event-callback-hook';
import { parseISOSafe } from 'utils/date-utils';
import { DateFieldProps } from './date-field-types';
import { useStyles } from './date-field-styles';

export function DateField({
  disabled,
  minDate: minDateString,
  maxDate: maxDateString,
  value,
  onChange,
}: DateFieldProps) {
  // fix undefined value
  value = value ?? null;

  const { locale } = useContext(LocaleContext);

  const [internalValue, setInternalValue] = useState<Date | null | undefined>(
    () => parseISOSafe(value)
  );

  const minDate = parseISOSafe(minDateString);
  const maxDate = parseISOSafe(maxDateString);

  const isError = useEventCallback((date: Date | null | undefined) => {
    if (date != null) {
      if (!isValid(date)) {
        return true;
      } else if (maxDate && date > maxDate) {
        return true;
      } else if (minDate && date < minDate) {
        return true;
      }
    }

    return false;
  });

  // update internal representation if supplied from above
  useEffect(() => setInternalValue(parseISOSafe(value)), [value]);

  // stores the new value in internal storage and propagates above only null and valid dates
  const handleChange = useEventCallback((date: Date | null) => {
    setInternalValue(date);

    if (date == null) {
      onChange(null);
    } else if (!isError(date)) {
      onChange(formatISO(date, { representation: 'date' }));
    }
  });

  const classes = useStyles();

  return (
    <MuiPickersUtilsProvider utils={DateFnsUtils} locale={locale.dateFnsLocale}>
      <KeyboardDatePicker
        InputProps={{
          classes,
        }}
        views={['date', 'month', 'year']}
        disableToolbar
        variant="inline"
        fullWidth={true}
        disabled={disabled}
        value={internalValue}
        format={locale.dateFormat}
        onChange={handleChange}
        InputAdornmentProps={{
          style: {
            display: 'none',
          },
        }}
        // don't show error messages only error underline
        invalidDateMessage=""
        maxDateMessage=""
        minDateMessage=""
        error={disabled ? false : isError(internalValue)}
      />
    </MuiPickersUtilsProvider>
  );
}
