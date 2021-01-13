import React, { ChangeEvent, forwardRef } from 'react';
import MuiTextField from '@material-ui/core/TextField';
import { useEventCallback } from 'utils/event-callback-hook';
import { TextAreaProps } from './text-area-types';
import { useStyles } from './text-area-styles';
import InputAdornment from '@material-ui/core/InputAdornment';

export const TextArea = forwardRef<HTMLDivElement, TextAreaProps>(
  function TextArea(
    {
      form,
      onChange,
      disabled,
      value,
      onBlur,
      onFocus,
      startAdornment,
      endAdornment,
      onChangeEvent,
      error = false,
      minRows = 3,
      maxRows = 12,
    },
    ref
  ) {
    // fix undefined value
    value = value ?? null;

    // prepare value for MUI component
    value = value ?? '';

    const handleChange = useEventCallback(
      (e: ChangeEvent<HTMLTextAreaElement>) => {
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
        inputProps={{
          form,
          spellCheck: 'true',
        }}
        ref={ref}
        disabled={disabled}
        value={value}
        onChange={handleChange}
        onBlur={onBlur}
        onFocus={onFocus}
        error={error}
        fullWidth={true}
        lang="cs"
        multiline={true}
        rows={minRows}
        rowsMax={maxRows}
      ></MuiTextField>
    );
  }
);
