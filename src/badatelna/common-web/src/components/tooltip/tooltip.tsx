import React, { PropsWithChildren } from 'react';
import MuiTooltip from '@material-ui/core/Tooltip';
import Typography from '@material-ui/core/Typography';
import ClickAwayListener from '@material-ui/core/ClickAwayListener';
import { TooltipProps } from './tooltip-types';
import { useStyles } from './tooltip-styles';
import { useTooltip } from './tooltip-hook';

export function Tooltip({
  children,
  title,
  placement,
  type = 'HOVER',
  ...childrenProps
}: PropsWithChildren<TooltipProps>) {
  const classes = useStyles();

  const { opened, openTooltip, closeTooltip, wrappedChildren } = useTooltip(
    type,
    children,
    childrenProps
  );

  if (type === 'HOVER') {
    return (
      <MuiTooltip
        interactive
        title={title !== undefined ? <Typography>{title}</Typography> : ''}
        classes={classes}
        placement={placement}
      >
        {wrappedChildren}
      </MuiTooltip>
    );
  } else {
    return (
      <MuiTooltip
        interactive
        title={
          title !== undefined ? (
            <ClickAwayListener onClickAway={closeTooltip}>
              <Typography>{title}</Typography>
            </ClickAwayListener>
          ) : (
            ''
          )
        }
        classes={classes}
        placement={placement}
        open={opened}
        onClick={title !== undefined ? openTooltip : undefined}
      >
        {wrappedChildren}
      </MuiTooltip>
    );
  }
}
