import React, { useState, useEffect, useContext } from 'react';
import DateFnsUtils from '@date-io/date-fns';
import { isValid } from 'date-fns';
import MuiPickersUtilsProvider from '@material-ui/pickers/MuiPickersUtilsProvider';
import { KeyboardDateTimePicker } from '@material-ui/pickers/DateTimePicker';
import { LocaleContext } from 'common/locale/locale-context';
import { useEventCallback } from 'utils/event-callback-hook';
import { parseISOSafe } from 'utils/date-utils';
import { DateTimeFieldProps } from './date-time-field-types';
import { useStyles } from './date-time-field-styles';
import clsx from 'clsx';
import { useIntl } from 'react-intl';

/**
 * Format data for java Instant, NOT LocalDateTime
 * @param param0
 */
export function DateTimeField({
  form,
  disabled,
  minDate: minDateString,
  maxDate: maxDateString,
  value,
  onChange,
}: DateTimeFieldProps) {
  // fix undefined value
  value = value ?? null;

  const intl = useIntl();

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
      onChange(date.toISOString());
    }
  });

  const classes = useStyles();

  return (
    <MuiPickersUtilsProvider utils={DateFnsUtils} locale={locale.dateFnsLocale}>
      <KeyboardDateTimePicker
        InputProps={{
          classes: {
            root: classes.root,
            input: classes.input,
          },
        }}
        inputProps={{
          form,
        }}
        autoOk={true}
        ampm={false}
        variant="dialog"
        fullWidth={true}
        disabled={disabled}
        value={internalValue}
        format={locale.dateTimeFormat}
        onChange={handleChange}
        InputAdornmentProps={{
          classes: {
            root: clsx({
              [classes.addorment]: !disabled,
              [classes.dissabledAddorment]: disabled,
            }),
          },
        }}
        cancelLabel={intl.formatMessage({
          id: 'EAS_DATEPICKER_BUTTON_CANCEL',
          defaultMessage: 'Zrušit',
        })}
        okLabel={intl.formatMessage({
          id: 'EAS_DATEPICKER_BUTTON_OK',
          defaultMessage: 'Potvrdit',
        })}
        // don't show error messages only error underline
        invalidDateMessage=""
        maxDateMessage=""
        minDateMessage=""
        error={disabled ? false : isError(internalValue)}
      />
    </MuiPickersUtilsProvider>
  );
}
