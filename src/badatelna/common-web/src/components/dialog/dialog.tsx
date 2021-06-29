import React, {
  forwardRef,
  useImperativeHandle,
  useState,
  MouseEvent,
} from 'react';
import { useEventCallback } from 'utils/event-callback-hook';
import { FormattedMessage } from 'react-intl';
import { noop, stubTrue } from 'lodash';
import MuiDialogActions from '@material-ui/core/DialogActions';
import MuiDialogContent from '@material-ui/core/DialogContent';
import MuiDialog from '@material-ui/core/Dialog';
import MuiDialogTitle from '@material-ui/core/DialogTitle';
import Typography from '@material-ui/core/Typography';
import CircularProgress from '@material-ui/core/CircularProgress';
import Button from '@material-ui/core/Button';
import ButtonGroup from '@material-ui/core/ButtonGroup';
import { DialogProps, DialogHandle } from './dialog-types';
import { useStyles } from './dialog-styles';
import { DragablePaper } from 'components/draggable-paper/draggable-paper';

export const Dialog = forwardRef<DialogHandle, DialogProps>(function Dialog(
  {
    title,
    children,
    onConfirm = stubTrue,
    onCancel = noop,
    onShow = noop,
    onShown = noop,
    showConfirm = true,
    confirmLabel,
    showClose = true,
    closeLabel,
    actions,
    loading = false,
    disableBackdrop = false,
  },
  ref
) {
  const classes = useStyles();
  const [opened, setOpened] = useState(false);

  const open = useEventCallback(() => {
    onShow();

    requestAnimationFrame(() => {
      onShown();
    });

    setOpened(true);
  });
  const close = useEventCallback(() => setOpened(false));

  const handleConfirm = useEventCallback(async () => {
    let result = onConfirm();

    if (result instanceof Promise) {
      result = await result;
    }

    if (result !== false) {
      close();
    }
  });

  const handleCancel = useEventCallback(() => {
    onCancel();
    close();
  });

  useImperativeHandle(
    ref,
    () => ({
      open,
      close,
    }),
    [close, open]
  );

  const handleDialogClick = useEventCallback((event: MouseEvent<any>) =>
    event.stopPropagation()
  );

  return (
    <MuiDialog
      onClick={handleDialogClick}
      open={opened}
      onClose={close}
      PaperComponent={DragablePaper}
      maxWidth="lg"
      disableBackdropClick={disableBackdrop}
    >
      <MuiDialogTitle
        disableTypography={true}
        classes={{ root: classes.title }}
      >
        <Typography className={classes.titleHeader} variant="h6">
          {title}
        </Typography>
      </MuiDialogTitle>
      <MuiDialogContent dividers={true}>
        {opened && children()}
      </MuiDialogContent>
      {(showConfirm || actions || showClose) && (
        <MuiDialogActions classes={{ root: classes.actions }}>
          <ButtonGroup size="small" variant="outlined">
            {showConfirm && (
              <Button
                type="submit"
                onClick={handleConfirm}
                variant="outlined"
                color="primary"
                disabled={loading}
                startIcon={
                  loading && <CircularProgress size="20px" color="inherit" />
                }
              >
                <Typography classes={{ root: classes.buttonLabel }}>
                  {confirmLabel ?? (
                    <FormattedMessage
                      id="EAS_DIALOG_BTN_CONFIRM"
                      defaultMessage="Potvrdit"
                    />
                  )}
                </Typography>
              </Button>
            )}

            {actions}

            {showClose && (
              <Button
                variant="outlined"
                onClick={handleCancel}
                disabled={loading}
              >
                <Typography classes={{ root: classes.buttonLabel }}>
                  {closeLabel ?? (
                    <FormattedMessage
                      id="EAS_DIALOG_BTN_CANCEL"
                      defaultMessage="Zrušit"
                    />
                  )}
                </Typography>
              </Button>
            )}
          </ButtonGroup>
        </MuiDialogActions>
      )}
    </MuiDialog>
  );
});
