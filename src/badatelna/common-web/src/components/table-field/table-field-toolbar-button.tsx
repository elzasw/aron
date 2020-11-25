import React, { PropsWithChildren } from 'react';
import { TableFieldToolbarButtonProps } from './table-field-types';
import IconButton from '@material-ui/core/IconButton';
import { useStyles } from './table-field-styles';
import { Tooltip } from 'components/tooltip/tooltip';

export function TableFieldToolbarButton({
  show,
  title,
  onClick,
  IconComponent,
  disabled,
  children,
  component = 'button',
  href,
}: PropsWithChildren<TableFieldToolbarButtonProps>) {
  const classes = useStyles();

  if (show) {
    return (
      <IconButton
        component={component}
        onClick={onClick}
        classes={{ root: classes.iconButton }}
        disabled={disabled}
        href={href}
      >
        <Tooltip {...{ title, placement: 'top' }}>
          <IconComponent color={disabled ? 'disabled' : 'primary'} />
        </Tooltip>
        {children}
      </IconButton>
    );
  } else {
    return <></>;
  }
}
