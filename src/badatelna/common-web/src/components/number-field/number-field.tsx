import React from 'react';
import { useRifm } from 'rifm';
import { useEventCallback } from 'utils/event-callback-hook';
import { useNumberAdapter } from 'utils/number-adapter-hook';
import { useInternalState } from 'utils/internal-state-hook';
import { NumberFieldProps } from './number-field-types';
import { TextField } from 'components/text-field/text-field';

export function NumberField({
  value: providedValue = null,
  onChange: providedOnChange,
  negative = false,
  formated = false,
  maxValue,
  minValue,
  ...props
}: NumberFieldProps) {
  const adapter = useNumberAdapter(providedValue, providedOnChange);

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
    adapter,
    valueCondition,
    changeCondition
  );

  const handleChange = useEventCallback((value: string) =>
    setValue(negative ? parseNegative(value) : parseInteger(value))
  );

  const accept = negative ? negativeAccept : integerAccept;
  const format = negative
    ? formated
      ? formatNegativeSeparator
      : formatNegative
    : formated
    ? formatIntegerSeparator
    : formatInteger;

  const rifm = useRifm({
    accept,
    format,
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

const integerAccept = /\d+/g;
function parseInteger(str: string) {
  return (str.match(integerAccept) || []).join('');
}

function formatInteger(str: string) {
  const parsed = parseInteger(str);
  const number = Number.parseInt(parsed, 10);
  if (Number.isNaN(number)) {
    return '';
  }
  return number.toString();
}

function formatIntegerSeparator(str: string) {
  const parsed = parseInteger(str);
  const number = Number.parseInt(parsed, 10);
  if (Number.isNaN(number)) {
    return '';
  }
  return number.toLocaleString('cs');
}

const negativeAccept = /[\d-]+/g;
function parseNegative(str: string) {
  return (str.match(negativeAccept) || []).join('');
}

function formatNegative(str: string) {
  const parsed = parseNegative(str);
  if (parsed === '-') {
    return '-';
  }
  const number = Number.parseInt(parsed, 10);
  if (Number.isNaN(number)) {
    return '';
  }
  return number.toString();
}

function formatNegativeSeparator(str: string) {
  const parsed = parseNegative(str);
  if (parsed === '-') {
    return '-';
  }
  const number = Number.parseInt(parsed, 10);
  if (Number.isNaN(number)) {
    return '';
  }
  return number.toLocaleString('cs');
}
