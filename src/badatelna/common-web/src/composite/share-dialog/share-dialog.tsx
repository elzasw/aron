import React, { forwardRef, useContext, useRef } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import FileCopyIcon from '@material-ui/icons/FileCopy';
import Typography from '@material-ui/core/Typography';
import TextField from '@material-ui/core/TextField';
import InputAdornment from '@material-ui/core/InputAdornment';
import IconButton from '@material-ui/core/IconButton';
import { Dialog } from 'components/dialog/dialog';
import { DialogHandle } from 'components/dialog/dialog-types';
import { ShareDialogProps } from './share-dialog-types';
import { DomainObject } from 'common/common-types';
import { DetailContext } from 'composite/detail/detail-context';
import { DetailHandle } from 'composite/detail/detail-types';
import { useEventCallback } from 'utils/event-callback-hook';
import { Tooltip } from 'components/tooltip/tooltip';
import { SnackbarContext } from 'composite/snackbar/snackbar-context';
import { SnackbarVariant } from 'composite/snackbar/snackbar-types';

export const ShareDialog = forwardRef<DialogHandle, ShareDialogProps>(
  function ShareDialog({ onCancel }, ref) {
    const inputRef = useRef<HTMLInputElement>(null);

    const intl = useIntl();
    const { source } = useContext<DetailHandle<DomainObject>>(DetailContext);
    const { showSnackbar } = useContext(SnackbarContext);

    const handleFocus = useEventCallback((e) => {
      e.preventDefault();
      if (inputRef.current) {
        const last = inputRef.current.value.length;
        inputRef.current.focus();
        inputRef.current.setSelectionRange(0, last);
      }
    });

    const handleCopy = useEventCallback((e) => {
      e.preventDefault();

      if (inputRef.current) {
        handleFocus(e);

        document.execCommand('copy');

        const message = intl.formatMessage({
          id: 'EAS_DETAIL_SHARE_DIALOG_MSG_SUCCESS',
          defaultMessage: 'Odkaz jste úspěšně zkopírovali',
        });

        showSnackbar(message, SnackbarVariant.SUCCESS);
      }
    });

    const title = (
      <FormattedMessage
        id="EAS_DETAIL_SHARE_DIALOG_TITLE"
        defaultMessage="Získat odkaz"
      />
    );

    const text = (
      <FormattedMessage
        id="EAS_DETAIL_SHARE_DIALOG_TEXT"
        defaultMessage="Sdílet s ostatními:"
      />
    );

    const copyTooltip = (
      <FormattedMessage
        id="EAS_DETAIL_SHARE_DIALOG_COPY_BUTTON"
        defaultMessage="Kopírovat"
      />
    );

    return (
      <Dialog ref={ref} title={title} showConfirm={false} onCancel={onCancel}>
        {() => (
          <>
            <Typography style={{ width: 900 }}>{text}</Typography>
            <br />
            {source.data?.id && (
              <TextField
                variant="outlined"
                fullWidth
                onClick={handleFocus}
                value={`${window.location.href}/${source.data.id}`}
                inputRef={inputRef}
                InputProps={{
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        aria-label="copy"
                        onClick={handleCopy}
                        edge="end"
                      >
                        <Tooltip title={copyTooltip} placement="right">
                          <FileCopyIcon />
                        </Tooltip>
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
              />
            )}
          </>
        )}
      </Dialog>
    );
  }
);
