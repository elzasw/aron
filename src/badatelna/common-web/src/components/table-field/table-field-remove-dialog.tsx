import React, { useContext, forwardRef } from 'react';
import Typography from '@material-ui/core/Typography';
import { TableFieldRemoveDialogProps } from './table-field-types';
import { TableFieldContext } from './table-field-context';
import { useEventCallback } from 'utils/event-callback-hook';
import { FormattedMessage } from 'react-intl';
import { Dialog } from 'components/dialog/dialog';
import { DialogHandle } from 'components/dialog/dialog-types';

export const TableFieldRemoveDialog = forwardRef<
  DialogHandle,
  TableFieldRemoveDialogProps
>(function TableFieldRemoveDialog({ index }, ref) {
  const { removeRow } = useContext(TableFieldContext);

  const handleRemove = useEventCallback(() => {
    removeRow(index);
  });

  return (
    <Dialog
      ref={ref}
      title={
        <FormattedMessage
          id="EAS_TABLE_FIELD_REMOVE_DIALOG_TITLE"
          defaultMessage="Smazání"
        />
      }
      onConfirm={handleRemove}
      confirmLabel={
        <FormattedMessage
          id="EAS_TABLE_FIELD_REMOVE_DIALOG_BTN_DELETE"
          defaultMessage="Smazat"
        />
      }
    >
      {() => (
        <Typography>
          <FormattedMessage
            id="EAS_TABLE_FIELD_REMOVE_DIALOG_MSG_CONFIRM"
            defaultMessage="Jste si jisti, že chcete smazat vybraný prvek ?"
          />
        </Typography>
      )}
    </Dialog>
  );
});
