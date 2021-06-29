import React, { useRef, forwardRef, Ref, ReactElement } from 'react';
import { Form } from 'composite/form/form';
import { useEventCallback } from 'utils/event-callback-hook';
import { FormHandle } from 'composite/form/form-types';
import { FormattedMessage } from 'react-intl';
import { Dialog } from 'components/dialog/dialog';
import { DialogHandle } from 'components/dialog/dialog-types';
import { CreateDialogProps } from './named-settings-types';
import { FormTextField } from 'composite/form/fields/form-text-field';
import { FormCheckbox } from 'composite/form/fields/form-checkbox';
import { NamedSettings } from 'common/settings/named/named-settings-types';

export const CreateDialog = forwardRef(function CreateDialog(
  { onConfirm }: CreateDialogProps,
  dialogRef: Ref<DialogHandle> | undefined | null
) {
  const ref = useRef<FormHandle<NamedSettings>>(null);

  const handleSubmit = useEventCallback((values: NamedSettings) => {
    onConfirm(values);
  });

  const handleSave = useEventCallback(() => {
    ref.current?.submitForm();
  });

  return (
    <Dialog
      ref={dialogRef}
      title={
        <FormattedMessage
          id="EAS_TABLE_NAMED_SETTINGS_DIALOG_TITLE_DETAIL"
          defaultMessage="Uložení nastavení"
        />
      }
      confirmLabel={
        <FormattedMessage
          id="EAS_TABLE_NAMED_SETTINGS_BTN_SAVE"
          defaultMessage="Uložit"
        />
      }
      onConfirm={handleSave}
    >
      {() => (
        <Form
          editing
          initialValues={{
            id: '',
            name: '',
            settings: '',
            tag: '',
            shared: false,
          }}
          onSubmit={handleSubmit}
          ref={ref}
        >
          <FormTextField
            name="name"
            label={
              <FormattedMessage
                id="EAS_TABLE_NAMED_SETTINGS_LABEL_FIELD_NAME"
                defaultMessage="Název"
              />
            }
          />
          <FormCheckbox
            name="shared"
            label={
              <FormattedMessage
                id="EAS_TABLE_NAMED_SETTINGS_LABEL_FIELD_SHARED"
                defaultMessage="Sdílené"
              />
            }
          />
        </Form>
      )}
    </Dialog>
  );
}) as (p: CreateDialogProps & { ref?: Ref<DialogHandle> }) => ReactElement;
