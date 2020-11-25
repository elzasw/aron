import React from 'react';
import classNames from 'classnames';
import MUIButton from '@material-ui/core/Button';

import { Props } from './types';
import { useStyles } from './styles';

export function Button({
  label,
  className,
  outlined,
  contained,
  ...props
}: Props) {
  const classes = useStyles();

  return (
    <MUIButton
      {...{
        ...props,
        className: classNames(classes.button, className),
        variant: outlined ? 'outlined' : contained ? 'contained' : 'text',
      }}
    >
      {label}
    </MUIButton>
  );
}
