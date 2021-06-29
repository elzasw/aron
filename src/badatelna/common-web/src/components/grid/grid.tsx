import React from 'react';
import Box from '@material-ui/core/Box';
import { GridProps } from './grid-types';
import { useStyles } from './grid-styles';

export function Grid({
  children,
  gap = 10,
  columns,
}: React.PropsWithChildren<GridProps>) {
  const classes = useStyles();
  return (
    <Box
      className={classes.root}
      gridTemplateColumns={`repeat(${columns}, 1fr)`}
      gridGap={gap}
    >
      {children}
    </Box>
  );
}
