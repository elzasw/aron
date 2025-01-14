import React, { forwardRef, Ref } from 'react';
import clsx from 'clsx';
import Button from '@material-ui/core/Button';
import { Tooltip } from 'components/tooltip/tooltip';
import { TableToolbarButtonProps } from './table-types';
import { useStyles } from './table-styles';

export const TableToolbarButton = forwardRef(function TableToolbarButton(
  {
    className,
    tooltip,
    disabled,
    checked,
    endIcon,
    onClick,
    href,
    label,
    primary = checked,
    secondary,
  }: TableToolbarButtonProps,
  ref?: Ref<HTMLSpanElement>
) {
  const classes = useStyles();

  return (
    <li className={classes.toolbarButtonWrapper}>
      <Tooltip title={tooltip} placement="top-start">
        <Button
          ref={ref}
          disabled={disabled}
          className={clsx(
            classes.toolbarText,
            classes.toolbarButton,
            className
          )}
          size="small"
          component={href ? 'a' : 'span'}
          onClick={onClick}
          href={href}
          endIcon={endIcon}
          color={primary ? 'primary' : secondary ? 'secondary' : 'default'}
          variant="contained"
          disableElevation
        >
          {label}
        </Button>
      </Tooltip>
    </li>
  );
});
