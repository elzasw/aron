import React from 'react';
import LinearProgress from '@material-ui/core/LinearProgress';

import { Props } from './types';
import { useStyles } from './styles';

export function Loading({ loading }: Props) {
  const classes = useStyles();

  return <div className={classes.loadingPlaceholder}>
    {loading ? <LinearProgress /> : <></>}
  </div>
}
