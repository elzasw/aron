import React, { useContext } from 'react';
import { FormattedMessage } from 'react-intl';
import RemoveIcon from '@material-ui/icons/Delete';
import GetAppIcon from '@material-ui/icons/GetApp';
import { FileRef } from 'common/common-types';
import { FilesContext } from 'common/files/files-context';
import { TableFieldToolbarButton } from 'components/table-field/table-field-toolbar-button';
import { TableFieldCellProps } from 'components/table-field/table-field-types';
import { TableFieldContext } from 'components/table-field/table-field-context';
import { useEventCallback } from 'utils/event-callback-hook';

export function FileTableActionCell(props: TableFieldCellProps<FileRef>) {
  const { getFileUrl } = useContext(FilesContext);

  const { disabled, showRemoveDialog } = useContext(TableFieldContext);

  const handleRemove = useEventCallback(() => {
    showRemoveDialog(props.index);
  });

  return (
    <>
      <TableFieldToolbarButton
        title={
          <FormattedMessage
            id="EAS_FILE_TABLE_BTN_DOWNLOAD"
            defaultMessage="Stáhnout"
          />
        }
        IconComponent={GetAppIcon}
        disabled={false}
        show={true}
        href={getFileUrl(props.rowValue.id)}
      />
      <TableFieldToolbarButton
        title={
          <FormattedMessage
            id="EAS_FILE_TABLE_BTN_REMOVE"
            defaultMessage="Smazat"
          />
        }
        IconComponent={RemoveIcon}
        disabled={disabled}
        show={true}
        onClick={handleRemove}
      />
    </>
  );
}
