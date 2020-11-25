import React, { PropsWithChildren } from 'react';
import Grid from '@material-ui/core/Grid';
import { PanelProps } from 'components/panel/panel-types';
import { Panel } from 'components/panel/panel';

export function FormPanel({
  children,
  ...props
}: PropsWithChildren<PanelProps>) {
  return (
    <Grid item xs={12}>
      <Panel {...props}>
        <Grid container spacing={0}>
          {children}
        </Grid>
      </Panel>
    </Grid>
  );
}
