import React, { forwardRef } from 'react';
import { Dialog } from 'components/dialog/dialog';
import { DialogHandle } from 'components/dialog/dialog-types';
import { MessageDialogProps } from './message-dialog-types';
import { FormattedMessage } from 'react-intl';

export const MessageDialog = forwardRef<DialogHandle, MessageDialogProps>(
  function MessageDialog({ title, content, onClose }, ref) {
    return (
      <Dialog
        ref={ref}
        title={title}
        onConfirm={onClose}
        showClose={false}
        confirmLabel={
          <FormattedMessage
            id="EAS_MESSAGE_DIALOG_BTN_CLOSE"
            defaultMessage="Zavřít"
          />
        }
      >
        {() => <>{content}</>}
      </Dialog>
    );
  }
);
