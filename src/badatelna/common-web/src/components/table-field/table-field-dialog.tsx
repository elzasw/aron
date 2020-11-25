import React, {
  useContext,
  useRef,
  forwardRef,
  Ref,
  ReactElement,
} from 'react';
import { TableFieldDialogProps } from './table-field-types';
import { TableFieldContext } from './table-field-context';
import { Form } from 'composite/form/form';
import { useEventCallback } from 'utils/event-callback-hook';
import { FormHandle } from 'composite/form/form-types';
import { FormattedMessage } from 'react-intl';
import { Dialog } from 'components/dialog/dialog';
import { DialogHandle } from 'components/dialog/dialog-types';

export const TableFieldDialog = forwardRef(function TableFieldDialog<OBJECT>(
  { index, value, FormFieldsComponent }: TableFieldDialogProps<OBJECT>,
  dialogRef: Ref<DialogHandle> | undefined | null
) {
  const { disabled, saveRow, initNewRow } = useContext<
    TableFieldContext<OBJECT>
  >(TableFieldContext);
  const ref = useRef<FormHandle<OBJECT>>(null);

  const handleSubmit = useEventCallback((values: OBJECT) => {
    saveRow(index, values);
  });

  const handleSave = useEventCallback(() => {
    ref.current?.submitForm();
  });

  const title = (
    <>
      {disabled && (
        <FormattedMessage
          id="EAS_TABLE_FIELD_DIALOG_TITLE_DETAIL"
          defaultMessage="Detail"
        />
      )}
      {!disabled && value !== null && (
        <FormattedMessage
          id="EAS_TABLE_FIELD_DIALOG_TITLE_EDIT"
          defaultMessage="Úprava"
        />
      )}
      {!disabled && value === null && (
        <FormattedMessage
          id="EAS_TABLE_FIELD_DIALOG_TITLE_ADD"
          defaultMessage="Přidání"
        />
      )}
    </>
  );

  return (
    <Dialog
      ref={dialogRef}
      title={title}
      showConfirm={!disabled}
      confirmLabel={
        <FormattedMessage
          id="EAS_TABLE_FIELD_BTN_SAVE"
          defaultMessage="Uložit"
        />
      }
      onConfirm={handleSave}
    >
      {() => (
        <Form
          editing={!disabled}
          initialValues={value ?? initNewRow()}
          onSubmit={handleSubmit}
          ref={ref}
        >
          <FormFieldsComponent initialValue={value} />
        </Form>
      )}
    </Dialog>
  );
}) as <OBJECT>(
  p: TableFieldDialogProps<OBJECT> & { ref?: Ref<DialogHandle> }
) => ReactElement;
