import React, { PropsWithChildren, useContext } from 'react';
import clsx from 'clsx';
import Grid from '@material-ui/core/Grid';
import Box from '@material-ui/core/Box';
import Typography from '@material-ui/core/Typography';
import InputLabel from '@material-ui/core/InputLabel';
import FormHelperText from '@material-ui/core/FormHelperText';
import { FormFieldWrapperProps } from './form-field-wrapper-types';
import { useStyles } from './form-field-wrapper-styles';
import { ContextHelp } from 'components/context-help/context-help';
import { useIntl, FormattedMessage } from 'react-intl';
import { HelpContext } from 'common/help/help-context';

export function FormFieldWrapper({
  children,
  label,
  helpLabel = ' ',
  required,
  disabled,
  labelOptions,
  layoutOptions,
  errorOptions,
  before,
  after,
  errors,
}: PropsWithChildren<FormFieldWrapperProps>) {
  const {
    underline,
    spacing,
    labelText,
    labelRoot,
    labelDisabled,
    labelError,
    boxRoot,
    boxError,
    labelBold,
    labelItalic,
  } = useStyles();
  const intl = useIntl();
  const { formContextHelpType } = useContext(HelpContext);

  if (required) {
    const requiredText = intl.formatMessage({
      id: 'EAS_FIELD_REQUIRED',
      defaultMessage: 'Povinné pole',
    });

    if (helpLabel === ' ') {
      helpLabel = requiredText;
    } else {
      helpLabel = requiredText + '<br/>' + helpLabel;
    }
  }

  const hideLabel = labelOptions.hide ?? false;
  const hideErrors = errorOptions.hide ?? false;

  const hasError = (errors?.length ?? 0) > 0;

  return (
    <Grid
      item
      xs={12}
      classes={{ root: clsx({ [underline]: !layoutOptions?.noUnderline }) }}
    >
      <Grid
        container
        spacing={0}
        alignItems="flex-start"
        classes={{ root: clsx({ [spacing]: !layoutOptions?.noSpacing }) }}
      >
        {!hideLabel ? (
          <>
            <Grid item xs={12} sm={4}>
              <Box className={clsx(boxRoot, { [boxError]: hasError })}>
                <InputLabel
                  disabled={disabled}
                  classes={{
                    root: labelRoot,
                    disabled: labelDisabled,
                  }}
                >
                  <Typography
                    variant="body2"
                    component="div"
                    classes={{
                      root: clsx(labelText, {
                        [labelBold]: required || labelOptions.bold,
                        [labelError]: hasError,
                        [labelItalic]: labelOptions.italic,
                      }),
                    }}
                  >
                    {label}
                  </Typography>
                </InputLabel>
                <ContextHelp type={formContextHelpType} label={helpLabel} />
              </Box>
            </Grid>
            <Grid item xs={12} sm={8}>
              <Box display="flex">
                {before}
                {children}
                {after}
              </Box>
              {!hideErrors &&
                errors?.map((error, index) => {
                  const [key, defaultMessage] = error.value.split(';;');

                  return (
                    <FormHelperText key={index} error={hasError}>
                      <FormattedMessage
                        id={key}
                        defaultMessage={defaultMessage}
                      />
                    </FormHelperText>
                  );
                })}
            </Grid>
          </>
        ) : (
          <Grid item xs={12} sm={12}>
            <Box display="flex">
              {before}
              {children}
              {after}
            </Box>
            {!hideErrors &&
              errors?.map((error, index) => {
                const [key, defaultMessage] = error.value.split(';;');

                return (
                  <FormHelperText key={index} error={hasError}>
                    <FormattedMessage
                      id={key}
                      defaultMessage={defaultMessage}
                    />
                  </FormHelperText>
                );
              })}
          </Grid>
        )}
      </Grid>
    </Grid>
  );
}
