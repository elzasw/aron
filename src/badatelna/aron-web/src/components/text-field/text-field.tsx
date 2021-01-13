import React from 'react';
import classNames from 'classnames';
import { useStyles } from './styles';

import MUITextField from '@material-ui/core/TextField';
import { Props } from './types';

export function TextField({
  value,
  onChange,
  className,
  variant = 'outlined',
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
        variant,
        onChange: handleChange,
        className: classNames(
          classes.textField,
          className,
          variant === 'outlined' && classes.textFieldOutlined
        ),
      }}
    />
  );
}
