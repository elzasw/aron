import React, { useContext } from 'react';
import MuiButton from '@material-ui/core/Button';
import Typography from '@material-ui/core/Typography';
import GetAppIcon from '@material-ui/icons/GetApp';
import { FileFieldProps } from './file-field-types';
import { useStyles } from './file-field-styles';
import { useEventCallback } from 'utils/event-callback-hook';
import { FilesContext } from 'common/files/files-context';
import { TextField } from 'components/text-field/text-field';
import { FormattedMessage } from 'react-intl';

export function FileField({ value, onChange, disabled }: FileFieldProps) {
  const classes = useStyles();
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

  return (
    <TextField
      disabled={true}
      value={value?.name ?? ''}
      startAdornment={
        <>
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
              style={{ display: 'none' }}
              onChange={handleUpload}
            />
          </MuiButton>
          {value && (
            <MuiButton
              download={true}
              size="small"
              classes={{ root: classes.downloadButton }}
              variant="contained"
              href={getFileUrl(value.id)}
            >
              <GetAppIcon />
            </MuiButton>
          )}
        </>
      }
    />
  );
}
