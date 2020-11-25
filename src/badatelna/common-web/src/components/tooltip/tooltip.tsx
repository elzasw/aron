import React, { PropsWithChildren } from 'react';
import MuiTooltip from '@material-ui/core/Tooltip';
import Typography from '@material-ui/core/Typography';
import { TooltipProps } from './tooltip-types';
import { useStyles } from './tooltip-styles';

export function Tooltip({
  children,
  title,
  placement,
  ...childrenProps
}: PropsWithChildren<TooltipProps>) {
  const child = React.Children.toArray(children)
    .filter(React.isValidElement)
    .map((child) => React.cloneElement(child, childrenProps))[0];

  const classes = useStyles();

  return (
    <MuiTooltip
      interactive
      title={title !== undefined ? <Typography>{title}</Typography> : ''}
      classes={classes}
      placement={placement}
    >
      {child}
    </MuiTooltip>
  );
}
