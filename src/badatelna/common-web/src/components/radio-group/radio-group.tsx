import React, { useMemo, ChangeEvent } from 'react';
import clsx from 'clsx';
import { uniqBy } from 'lodash';
import { DomainObject } from 'common/common-types';
import { RadioGroupProps } from './radio-group-types';
import { useEventCallback } from 'utils/event-callback-hook';
import MuiRadioGroup from '@material-ui/core/RadioGroup';
import MuiRadio from '@material-ui/core/Radio';
import MuiFormControlLabel from '@material-ui/core/FormControlLabel';
import { useStyles } from './radio-group-styles';

export function RadioGroup<OPTION extends DomainObject>({
  source,
  disabled,
  onChange,
  value,
  valueIsId = false,
  idMapper = (option: OPTION) => option.id,
  labelMapper = (option: OPTION) => (option as any).name,
  form,
}: RadioGroupProps<OPTION>) {
  const classes = useStyles();
  value = value ?? null;
  type VALUE = OPTION | string;

  const valueToId = useEventCallback((value: VALUE) => {
    return valueIsId ? (value as string) : idMapper(value as OPTION);
  });

  const idToValue = useEventCallback((id: string) => {
    return valueIsId ? id : options.find((option) => idMapper(option) === id)!;
  });

  // in some cases the value is not included in the options, adds it at the end
  const options: OPTION[] = useMemo(() => {
    if (value && !valueIsId) {
      return uniqBy([...source.items, value as OPTION], valueToId);
    } else {
      return source.items;
    }
  }, [value, source, valueIsId, valueToId]);

  const handleChange = useEventCallback(
    async (e: ChangeEvent<{ name?: string; value: unknown }>) => {
      const id = e.target.value;

      let value: VALUE | null;
      if (id === undefined || id === '') {
        value = null;
      } else {
        value = idToValue(id as string);
        if (!valueIsId) {
          value = await source.loadDetail(value as any);
        }
      }
      onChange(value);
    }
  );

  function getLocalValue(value: VALUE | null) {
    if (value === null) {
      return '';
    }

    const optionIds = options.map((option) => idMapper(option));

    const id = valueToId(value as VALUE);
    return optionIds.includes(id) ? id : '';
  }

  return (
    <MuiRadioGroup value={getLocalValue(value)} onChange={handleChange}>
      {options.map((option) => (
        <MuiFormControlLabel
          key={idMapper(option)}
          disabled={disabled}
          value={idMapper(option)}
          label={labelMapper(option)}
          form={form}
          classes={{
            label: classes.labelLabel,
            root: classes.labelRoot,
          }}
          control={
            <MuiRadio
              color="primary"
              className={clsx(classes.radioRoot, {
                [classes.highlightedIcon]: !disabled,
              })}
            />
          }
          className={classes.radioControl}
        />
      ))}
    </MuiRadioGroup>
  );
}
