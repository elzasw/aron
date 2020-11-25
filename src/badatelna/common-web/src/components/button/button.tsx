import React from 'react';
import { useEventCallback } from 'utils/event-callback-hook';
import MuiButton from '@material-ui/core/Button';
import { ButtonProps } from './button-types';
import { useStyles } from './button-styles';
import { Tooltip } from 'components/tooltip/tooltip';

export function Button({
  onClick,
  label,
  disabled,
  outlined = false,
  contained = false,
  tooltip,
}: ButtonProps) {
  const handleClick = useEventCallback((event: React.MouseEvent) => {
    if (onClick !== undefined) {
      onClick();
    }

    event.stopPropagation();
  });

  const classes = useStyles();

  return (
    <Tooltip title={tooltip}>
      <MuiButton
        classes={classes}
        disabled={disabled}
        onClick={handleClick}
        variant={outlined ? 'outlined' : contained ? 'contained' : 'text'}
      >
        {label}
      </MuiButton>
    </Tooltip>
  );
}
