import React from 'react';
import classNames from 'classnames';
import { useStyles } from './styles';

import MUITextField from '@material-ui/core/TextField';
import { Props } from './types';

export function TextField({
  value,
  onChange,
  className,
  variant,
  ...props
}: Props) {
  const classes = useStyles();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) =>
    onChange(e.target.value);

  return (
    <MUITextField
      {...{
        ...props,
        value,
        variant: variant || 'outlined',
        onChange: handleChange,
        className: classNames(classes.textField, className),
      }}
    />
  );
}
