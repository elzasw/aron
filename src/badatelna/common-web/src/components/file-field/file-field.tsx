import React, { useContext } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import MuiButton from '@material-ui/core/Button';
import Typography from '@material-ui/core/Typography';
import GetAppIcon from '@material-ui/icons/GetApp';
import ClearIcon from '@material-ui/icons/Clear';
import { FileFieldProps } from './file-field-types';
import { useStyles } from './file-field-styles';
import { useEventCallback } from 'utils/event-callback-hook';
import { FilesContext } from 'common/files/files-context';
import { TextField } from 'components/text-field/text-field';
import { SnackbarContext } from 'composite/snackbar/snackbar-context';
import { SnackbarVariant } from 'composite/snackbar/snackbar-types';

export function FileField({
  value,
  onChange,
  disabled,
  showUpload = true,
  showClear = true,
  accept = [],
  customDownloadUrl,
}: FileFieldProps) {
  const classes = useStyles();
  const { showSnackbar } = useContext(SnackbarContext);
  const intl = useIntl();
  const { uploadFile, getFileUrl } = useContext(FilesContext);

  const handleUpload = useEventCallback(
    async (event: React.ChangeEvent<any>) => {
      const input = event.currentTarget;
      const files: File[] = event.currentTarget.files;

      if (files.length === 0) {
        onChange(null);
        return;
      }

      const file = files[0];

      /**
       * Check the extension of a file and show error message
       * if not included in the accept array
       */

      const extensionIsValid = Boolean(
        accept.find((ext) => file.name.endsWith(ext))
      );
      if (accept.length && !extensionIsValid) {
        onChange(null);
        const message = intl.formatMessage(
          {
            id: 'EAS_FILES_MSG_ERROR_FILE_TYPE',
            defaultMessage:
              'Nepovolený typ souboru. Povolené hodnoty jsou: {fileTypes}',
          },
          {
            fileTypes: accept.join(','),
          }
        );
        showSnackbar(message, SnackbarVariant.ERROR);
        return;
      }

      const fileRef = await uploadFile(file);

      /*
        Reset file input, so one can load the same file again with trigering the onChange event.
        The event will not be triggered otherwise for the same file.
      */
      input.value = null;

      if (fileRef !== undefined) {
        onChange(fileRef);
      } else {
        onChange(null);
      }
    }
  );

  const handleRemove = useEventCallback(() => {
    onChange(null);
  });

  return (
    <TextField
      disabled={true}
      value={value?.name ?? ''}
      startAdornment={
        <>
          {showUpload && !disabled && (
            <MuiButton
              size="small"
              classes={{ root: classes.uploadButton }}
              disabled={disabled}
              variant="contained"
              component="label"
            >
              <Typography>
                <FormattedMessage
                  id="EAS_FILE_BTN_SELECT"
                  defaultMessage="Výběr"
                />
              </Typography>
              <input
                type="file"
                accept={accept.join(',')}
                style={{ display: 'none' }}
                onChange={handleUpload}
              />
            </MuiButton>
          )}
          {value && (
            <MuiButton
              download={true}
              size="small"
              classes={{ root: classes.downloadButton }}
              variant="contained"
              href={
                customDownloadUrl ? customDownloadUrl : getFileUrl(value.id)
              }
            >
              <GetAppIcon />
            </MuiButton>
          )}
          {showClear && value && !disabled && (
            <MuiButton
              size="small"
              classes={{ root: classes.downloadButton }}
              variant="contained"
              onClick={handleRemove}
              disabled={disabled}
            >
              <ClearIcon />
            </MuiButton>
          )}
        </>
      }
    />
  );
}
