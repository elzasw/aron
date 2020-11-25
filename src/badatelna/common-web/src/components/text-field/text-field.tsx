import React, { ChangeEvent, forwardRef } from 'react';
import MuiTextField from '@material-ui/core/TextField';
import InputAdornment from '@material-ui/core/InputAdornment';
import { useEventCallback } from 'utils/event-callback-hook';
import { TextFieldProps } from './text-field-types';
import { useStyles } from './text-field-styles';

export const TextField = forwardRef<HTMLDivElement, TextFieldProps>(
  function TextField(
    {
      id,
      name,
      onChange,
      disabled,
      value,
      onBlur,
      onFocus,
      onKeyDown,
      startAdornment,
      endAdornment,
      onChangeEvent,
      error = false,
      type = 'text',
      autocomplete,
    }: TextFieldProps,
    ref
  ) {
    // fix undefined value
    value = value ?? null;

    // prepare value for MUI component
    value = value ?? '';

    const handleChange = useEventCallback(
      (e: ChangeEvent<HTMLInputElement>) => {
        if (onChangeEvent !== undefined) {
          onChangeEvent(e);
        }

        if (onChange !== undefined) {
          const value = e.target.value;
          onChange(value !== '' ? value : null);
        }
      }
    );

    const classes = useStyles();

    startAdornment = startAdornment && (
      <InputAdornment position="start">{startAdornment}</InputAdornment>
    );
    endAdornment = endAdornment && (
      <InputAdornment position="end">{endAdornment}</InputAdornment>
    );

    return (
      <MuiTextField
        InputProps={{
          classes,
          startAdornment,
          endAdornment,
        }}
        id={id}
        name={name}
        ref={ref}
        disabled={disabled}
        value={value}
        onChange={handleChange}
        onBlur={onBlur}
        onFocus={onFocus}
        onKeyDown={onKeyDown}
        error={error}
        spellCheck={true}
        fullWidth={true}
        lang="cs"
        type={type}
        autoComplete={autocomplete}
      ></MuiTextField>
    );
  }
);
