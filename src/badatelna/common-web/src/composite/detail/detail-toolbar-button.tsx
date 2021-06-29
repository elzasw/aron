import React from 'react';
import Button, { ButtonProps } from '@material-ui/core/Button';
import { Tooltip } from 'components/tooltip/tooltip';
import {
  DetailToolbarButtonProps,
  DetailToolbarButtonType,
} from './detail-types';
import { useStyles } from './detail-styles';

export function DetailToolbarButton({
  label,
  tooltip,
  disabled,
  onClick,
  href,
  type = DetailToolbarButtonType.NORMAL,
  startIcon,
  endIcon,
  ...restProps
}: DetailToolbarButtonProps) {
  const classes = useStyles();

  const buttonProps: ButtonProps = { ...restProps };

  if (type === DetailToolbarButtonType.PRIMARY) {
    buttonProps.color = 'primary';
    buttonProps.variant = 'contained';
    buttonProps.classes = { root: classes.toolbarMainButton };
  } else if (type === DetailToolbarButtonType.SECONDARY) {
    buttonProps.color = 'secondary';
    buttonProps.variant = 'contained';
    buttonProps.classes = { root: classes.toolbarMainButton };
  }

  return (
    <Tooltip title={tooltip} placement="top-start">
      <Button
        onClick={onClick}
        href={href}
        disabled={disabled}
        classes={{ root: classes.toolbarButton }}
        {...buttonProps}
        variant="outlined"
        startIcon={startIcon}
        endIcon={endIcon}
      >
        {label}
      </Button>
    </Tooltip>
  );
}
