import React, { PropsWithChildren } from 'react';
import clsx from 'clsx';
import Grid from '@material-ui/core/Grid';
import Box from '@material-ui/core/Box';
import Typography from '@material-ui/core/Typography';
import InputLabel from '@material-ui/core/InputLabel';
import { FormFieldWrapperProps } from './form-field-wrapper-types';
import { useStyles } from './form-field-wrapper-styles';

export function FormFieldWrapper({
  children,
  label,
  required,
  disabled,
  labelOptions,
  before,
  after,
}: PropsWithChildren<FormFieldWrapperProps>) {
  const {
    underline,
    spacing,
    labelRoot,
    labelDisabled,
    labelBold,
    labelItalic,
  } = useStyles();

  const hideLabel = labelOptions.hide ?? false;

  return (
    <Grid item xs={12} classes={{ root: underline }}>
      <Grid
        container
        spacing={0}
        alignItems="center"
        classes={{ root: spacing }}
      >
        {!hideLabel ? (
          <>
            <Grid item xs={12} sm={4}>
              <InputLabel
                disabled={disabled}
                required={required}
                classes={{
                  root: labelRoot,
                  disabled: labelDisabled,
                }}
              >
                <Typography
                  variant="body2"
                  classes={{
                    root: clsx({
                      [labelBold]: labelOptions.bold,
                      [labelItalic]: labelOptions.italic,
                    }),
                  }}
                >
                  {label}
                </Typography>
              </InputLabel>
            </Grid>
            <Grid item xs={12} sm={8}>
              <Box display="flex">
                {before}
                {children}
                {after}
              </Box>
            </Grid>
          </>
        ) : (
          <Grid item xs={12} sm={12}>
            {children}
          </Grid>
        )}
      </Grid>
    </Grid>
  );
}
