import React, { PropsWithChildren, forwardRef } from 'react';
import Grid from '@material-ui/core/Grid';
import { PanelProps, PanelHandle } from 'components/panel/panel-types';
import { Panel } from 'components/panel/panel';

export const FormPanel = forwardRef<PanelHandle, PropsWithChildren<PanelProps>>(
  function FormPanel({ children, ...props }, ref) {
    return (
      <Grid item xs={12}>
        <Panel {...props} ref={ref}>
          <Grid container spacing={0}>
            {children}
          </Grid>
        </Panel>
      </Grid>
    );
  }
);
