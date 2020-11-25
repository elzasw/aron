import React, { useContext } from 'react';
import { FormattedMessage } from 'react-intl';
import ControlPointIcon from '@material-ui/icons/ControlPoint';
import { FilesContext } from 'common/files/files-context';
import { useStyles } from 'components/table-field/table-field-styles';
import { TableFieldContext } from 'components/table-field/table-field-context';
import { TableFieldToolbarButton } from 'components/table-field/table-field-toolbar-button';
import { useEventCallback } from 'utils/event-callback-hook';

export function FileTableToolbar() {
  const classes = useStyles();

  const { disabled, disabledAdd, visibleAdd, saveRow } = useContext(
    TableFieldContext
  );
  const { uploadFile } = useContext(FilesContext);

  const handleUpload = useEventCallback(
    async (event: React.ChangeEvent<any>) => {
      const files: File[] = event.currentTarget.files;

      for (const file of files) {
        const fileRef = await uploadFile(file);
        saveRow(undefined, fileRef);
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
