import React from 'react';
import { useRifm } from 'rifm';
import Big from 'big.js';
import { useEventCallback } from 'utils/event-callback-hook';
import { useInternalState } from 'utils/internal-state-hook';
import { DecimalFieldProps } from './decimal-field-types';
import { TextField } from 'components/text-field/text-field';

export function DecimalField({
  value: providedValue = null,
  onChange: providedOnChange,
  negative = false,
  formated = false,
  maxValue,
  minValue,
  ...props
}: DecimalFieldProps) {
  providedValue = providedValue != null ? providedValue : '0';

  const valueCondition = useEventCallback(
    (value: string, externalValue: string) => {
      return value !== externalValue;
    }
  );

  const changeCondition = useEventCallback((value: string) => {
    if (value === '') {
      return true;
    } else {
      const number = Number(value);
      if (!Number.isNaN(number)) {
        if (maxValue && number > maxValue) {
          return false;
        } else if (minValue && number < minValue) {
          return false;
        } else {
          return true;
        }
      } else {
        return false;
      }
    }
  });

  const [value, setValue, synced] = useInternalState(
    { value: providedValue, onChange: providedOnChange },
    valueCondition,
    changeCondition
  );

  const handleChange = useEventCallback((value: string) =>
    setValue(parseNumber(value))
  );

  const accept = /[\d.]/g;
  const format = formatFloatingPointNumber;

  const rifm = useRifm({
    accept,
    format: (v) => format(v, 2),
    value,
    onChange: handleChange,
  });

  return (
    <TextField
      value={rifm.value}
      onChangeEvent={rifm.onChange}
      error={!synced}
      {...props}
    />
  );
}

const numberAccept = /[\d.]+/g;

const parseNumber = (str: string) => (str.match(numberAccept) || []).join('');

const formatFloatingPointNumber = (value: string, maxDigits: number) => {
  const parsed = parseNumber(value);

  const big = Big(parsed);

  const formatted = big.toFixed(maxDigits);

  return formatted;
};
