import React from 'react';

import { Module } from '../module';
import { ModuleProps } from '../module';
import { useStyles } from './styles';

export function EvidenceWrapper({ children, ...props }: ModuleProps) {
  const classes = useStyles();

  return (
    <Module {...props}>
      <div className={classes.evidence}>{children}</div>
    </Module>
  );
}
