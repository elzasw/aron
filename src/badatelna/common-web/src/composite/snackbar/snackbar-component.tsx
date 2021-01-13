import React, { useState, forwardRef, useImperativeHandle } from 'react';
import { useEventCallback } from 'utils/event-callback-hook';
import clsx from 'clsx';
import Snackbar from '@material-ui/core/Snackbar';
import SnackbarContent from '@material-ui/core/SnackbarContent';
import IconButton from '@material-ui/core/IconButton';
import CloseIcon from '@material-ui/icons/Close';
import CheckCircleIcon from '@material-ui/icons/CheckCircle';
import ErrorIcon from '@material-ui/icons/Error';
import InfoIcon from '@material-ui/icons/Info';
import WarningIcon from '@material-ui/icons/Warning';
import { SnackbarVariant, SnackbarHandle } from './snackbar-types';
import { useStyles } from './snackbar-styles';
import { useTimeout } from 'utils/timeout-hook';

/**
 * Internal snackbar state.
 */
interface SnackbarState {
  message: string;
  variant: SnackbarVariant;
  open: boolean;
}

const variantIcon = {
  success: CheckCircleIcon,
  warning: WarningIcon,
  error: ErrorIcon,
  info: InfoIcon,
};

export const SnackbarComponent = forwardRef<
  SnackbarHandle,
  { timeout: number }
>(function SnackbarComponent({ timeout }, ref) {
  const classes = useStyles();
  const [triggerTimeout, cancelTimeout] = useTimeout();

  const [snackbar, setSnackbar] = useState<SnackbarState>({
    message: '',
    variant: SnackbarVariant.SUCCESS,
    open: false,
  });

  /**
   * Closes notification snackbar.
   */
  const hideSnackbar = useEventCallback(() => {
    setSnackbar((snackbar) => ({
      ...snackbar,
      open: false,
    }));
  });

  const showSnackbar = useEventCallback(
    (message: string, variant: SnackbarVariant) => {
      cancelTimeout();

      setSnackbar({ message, variant, open: true });

      if (
        timeout !== 0 &&
        (variant === SnackbarVariant.SUCCESS ||
          variant === SnackbarVariant.INFO)
      ) {
        triggerTimeout(() => {
          hideSnackbar();
        }, timeout);
      }
    }
  );

  useImperativeHandle(ref, () => ({ showSnackbar }), [showSnackbar]);

  const { variant, open, message } = snackbar;

  const Icon = variantIcon[variant];

  return (
    <Snackbar
      anchorOrigin={{
        vertical: 'bottom',
        horizontal: 'left',
      }}
      classes={{ root: classes.snackbar }}
      open={open}
    >
      <SnackbarContent
        className={classes[variant]}
        aria-describedby="client-snackbar"
        message={
          <span id="client-snackbar" className={classes.message}>
            <Icon className={clsx(classes.icon, classes.iconVariant)} />
            {message}
          </span>
        }
        action={[
          <IconButton
            key="close"
            aria-label="Close"
            color="inherit"
            onClick={hideSnackbar}
            size="small"
          >
            <CloseIcon className={classes.icon} />
          </IconButton>,
        ]}
      />
    </Snackbar>
  );
});
