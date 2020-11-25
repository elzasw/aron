import React, { forwardRef, Ref } from 'react';
import clsx from 'clsx';
import Button from '@material-ui/core/Button';
import { Tooltip } from 'components/tooltip/tooltip';
import { TableToolbarButtonProps } from './table-types';
import { useStyles } from './table-styles';

export const TableToolbarButton = forwardRef(function TableToolbarButton(
  {
    tooltip,
    disabled,
    checked,
    endIcon,
    onClick,
    label,
    primary,
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
          className={clsx(classes.toolbarText, classes.toolbarButton, {
            [classes.toolbarButtonPrimary]: primary,
          })}
          size="small"
          component="span"
          onClick={onClick}
          endIcon={endIcon}
          color={checked ? 'primary' : 'default'}
          variant={checked ? 'contained' : 'text'}
        >
          {label}
        </Button>
      </Tooltip>
    </li>
  );
});
