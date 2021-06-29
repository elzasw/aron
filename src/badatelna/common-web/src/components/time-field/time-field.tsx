import React, { useState, useEffect, useContext } from 'react';
import { useIntl } from 'react-intl';
import clsx from 'clsx';
import DateFnsUtils from '@date-io/date-fns';
import { isValid } from 'date-fns';
import MuiPickersUtilsProvider from '@material-ui/pickers/MuiPickersUtilsProvider';
import { KeyboardTimePicker } from '@material-ui/pickers/TimePicker';
import { LocaleContext } from 'common/locale/locale-context';
import { useEventCallback } from 'utils/event-callback-hook';
import { parseISOTimeSafe, formatISOTime } from 'utils/date-utils';
import { TimeFieldProps } from './time-field-types';
import { useStyles } from './time-field-styles';

export function TimeField({
  form,
  disabled,
  minTime: minTimeString,
  maxTime: maxTimeString,
  value,
  onChange,
}: TimeFieldProps) {
  // fix undefined value
  value = value ?? null;

  const intl = useIntl();

  const { locale } = useContext(LocaleContext);

  const [internalValue, setInternalValue] = useState<Date | null | undefined>(
    () => parseISOTimeSafe(value)
  );

  const minTime = parseISOTimeSafe(minTimeString);
  const maxTime = parseISOTimeSafe(maxTimeString);

  const isError = useEventCallback((date: Date | null | undefined) => {
    if (date != null) {
      if (!isValid(date)) {
        return true;
      } else if (maxTime && date > maxTime) {
        return true;
      } else if (minTime && date < minTime) {
        return true;
      }
    }

    return false;
  });

  // update internal representation if supplied from above
  useEffect(() => setInternalValue(parseISOTimeSafe(value)), [value]);

  // stores the new value in internal storage and propagates above only null and valid dates
  const handleChange = useEventCallback((date: Date | null) => {
    setInternalValue(date);

    if (date == null) {
      onChange(null);
    } else if (!isError(date)) {
      onChange(formatISOTime(date));
      console.log('time', formatISOTime(date));
    }
  });

  const classes = useStyles();

  return (
    <MuiPickersUtilsProvider utils={DateFnsUtils} locale={locale.dateFnsLocale}>
      <KeyboardTimePicker
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
        format={locale.timeFormat}
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
