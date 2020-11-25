import React from 'react';

import { Props } from './types';
import { AppHeader } from '..';
import { useStyles } from './styles';

export function AppWrapper({ children }: Props) {
  const classes = useStyles();

  return (
    <div className={classes.appWrapper}>
      <AppHeader />
      {children}
    </div>
  );
}
