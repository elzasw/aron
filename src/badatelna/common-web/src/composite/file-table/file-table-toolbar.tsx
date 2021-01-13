import React, { useContext } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import ControlPointIcon from '@material-ui/icons/ControlPoint';
import { FilesContext } from 'common/files/files-context';
import { useStyles } from 'components/table-field/table-field-styles';
import { TableFieldContext } from 'components/table-field/table-field-context';
import { TableFieldToolbarButton } from 'components/table-field/table-field-toolbar-button';
import { useEventCallback } from 'utils/event-callback-hook';
import { SnackbarContext } from 'composite/snackbar/snackbar-context';
import { SnackbarVariant } from 'index';
import { FileTableContext } from './file-table-context';

export function FileTableToolbar() {
  const classes = useStyles();

  const { disabled, disabledAdd, visibleAdd, saveRow, value } = useContext(
    TableFieldContext
  );
  const intl = useIntl();
  const { uploadFile } = useContext(FilesContext);
  const { maxItems } = useContext(FileTableContext);
  const { showSnackbar } = useContext(SnackbarContext);

  const handleUpload = useEventCallback(
    async (event: React.ChangeEvent<any>) => {
      const input = event.currentTarget;
      const files: File[] = event.currentTarget.files;

      try {
        if (maxItems !== undefined && value.length + files.length > maxItems) {
          const message = intl.formatMessage({
            id: 'EAS_FILE_TABLE_MSG_ERROR_UPLOAD_COUNT',
            defaultMessage: 'Byl překročen maximální povolený počet souborů',
          });

          showSnackbar(message, SnackbarVariant.ERROR);

          throw new Error('Max file size error');
        }

        for (const file of files) {
          const fileRef = await uploadFile(file);
          saveRow(undefined, fileRef);
        }
      } finally {
        /*
          Reset file input, so one can load the same file again with trigering the onChange event.
          The event will not be triggered otherwise for the same file.
        */
        input.value = null;
      }
    }
  );

  return (
    <div className={classes.tableActions}>
      <div className={classes.buttonGroup}>
        <TableFieldToolbarButton
          title={
            <FormattedMessage
              id="EAS_FILE_TABLE_BTN_ADD"
              defaultMessage="Přidat"
            />
          }
          IconComponent={ControlPointIcon}
          disabled={disabled || disabledAdd}
          show={visibleAdd}
          component="label"
        >
          <input
            type="file"
            multiple
            style={{ display: 'none' }}
            onChange={handleUpload}
          />
        </TableFieldToolbarButton>
      </div>
    </div>
  );
}
