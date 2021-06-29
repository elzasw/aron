import React from 'react';
import { DomainObject } from '../../common/common-types';
import { CheckboxGroupProps } from './checkbox-group-types';
import MuiFormControlLabel from '@material-ui/core/FormControlLabel';
import { useStyles } from './checkbox-group-styles';
import { useEventCallback } from 'utils/event-callback-hook';
import { Checkbox } from 'components/checkbox/checkbox';

export function CheckboxGroup<OPTION extends DomainObject>({
  source,
  disabled,
  onChange,
  idMapper = (option: OPTION) => option.id,
  labelMapper = (option: OPTION) => (option as any).name,
  form,
  value,
}: CheckboxGroupProps<OPTION>) {
  const classes = useStyles();

  const handleChange = useEventCallback((option) => {
    if (value?.includes(option)) {
      onChange(value?.filter((val) => val !== option));
    } else {
      onChange([...(value ?? []), option]);
    }
  });

  return (
    <div className={classes.checkboxGroup}>
      {source.items.map((option) => {
        const id = idMapper(option);

        return (
          <MuiFormControlLabel
            key={id}
            value={id}
            label={labelMapper(option)}
            disabled={disabled}
            form={form}
            classes={{
              label: classes.labelLabel,
              root: classes.labelRoot,
            }}
            control={
              <Checkbox
                key={id}
                value={value?.includes(id) ?? false}
                onChange={() => handleChange(id)}
              />
            }
            className={classes.checkboxControl}
          />
        );
      })}
    </div>
  );
}
