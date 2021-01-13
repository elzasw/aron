import React, { ChangeEvent, MouseEvent } from 'react';
import clsx from 'clsx';
import MuiCheckbox from '@material-ui/core/Checkbox';
import CheckBoxOutlinedIcon from '@material-ui/icons/CheckBoxSharp';
import CheckBoxOutlineBlankSharpIcon from '@material-ui/icons/CheckBoxOutlineBlankSharp';
import IndeterminateCheckBoxSharpIcon from '@material-ui/icons/IndeterminateCheckBoxSharp';
import { useEventCallback } from 'utils/event-callback-hook';
import { CheckboxProps } from './checkbox-types';
import { useStyles } from './checkbox-styles';

export function Checkbox({
  form,
  value,
  onChange,
  disabled,
  threeState,
  icon = <CheckBoxOutlineBlankSharpIcon />,
  checkedIcon = <CheckBoxOutlinedIcon />,
  highlighted = true,
}: CheckboxProps) {
  // fix undefined value
  value = value ?? null;

  // prepare value for MUI component
  const indeterminate = threeState ? value === null : false;
  value = !!value;

  const handleChange = useEventCallback((e: ChangeEvent<HTMLInputElement>) => {
    onChange(e.target.checked);
  });

  const handleClick = useEventCallback((e: MouseEvent<HTMLButtonElement>) => {
    e.stopPropagation();
  });

  const { root, highlightedRoot, ...classes } = useStyles();

  return (
    <MuiCheckbox
      classes={{
        root: clsx(root, { [highlightedRoot]: highlighted }),
        ...classes,
      }}
      inputProps={{
        form,
      }}
      indeterminate={indeterminate}
      disabled={disabled}
      checked={value}
      color="primary"
      onChange={handleChange}
      onClick={handleClick}
      icon={icon}
      checkedIcon={checkedIcon}
      indeterminateIcon={<IndeterminateCheckBoxSharpIcon />}
    />
  );
}
