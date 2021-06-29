import React, { forwardRef, useRef, RefObject } from 'react';
import { noop } from 'lodash';
import Typography from '@material-ui/core/Typography';
import { PromptDialogProps } from './prompt-types';
import { DialogHandle } from 'components/dialog/dialog-types';
import { FormHandle } from 'composite/form/form-types';
import { useEventCallback } from 'utils/event-callback-hook';
import { Dialog } from 'components/dialog/dialog';
import { Form } from 'composite/form/form';

export const PromptDialog = forwardRef<DialogHandle, PromptDialogProps>(
  function PromptDialog({ prompt, onConfirm, onCancel, loading }, ref) {
    const formRef = useRef<FormHandle<any>>(null);

    const handleConfirm = useEventCallback(async () => {
      if (!formRef.current) {
        onConfirm(undefined, formRef);

        return true;
      }

      const errors = await formRef.current?.validateForm();

      if (errors?.length === 0) {
        const values = formRef.current?.getFieldValues();

        // dialog stays opened if result === true
        const result = await onConfirm(values, formRef);

        return !result;
      } else {
        return false;
      }
    });

    const handleCancel = useEventCallback(() => {
      if (ref !== null) {
        (ref as RefObject<DialogHandle>).current?.close();
      }

      onCancel();
    });

    return (
      <Dialog
        ref={ref}
        title={prompt?.dialogTitle}
        showConfirm={prompt?.dialogShowConfirm ?? true}
        onConfirm={handleConfirm}
        showClose={prompt?.dialogShowClose ?? true}
        onCancel={onCancel}
        loading={loading}
      >
        {() => (
          <div style={{ width: prompt?.dialogWidth ?? 400 }}>
            <Typography>{prompt?.dialogText}</Typography>
            <br />
            {prompt?.FormFields !== undefined && (
              <Form
                ref={formRef}
                editing
                initialValues={prompt?.formInitialValues ?? {}}
                onSubmit={prompt?.formOnSubmit ?? noop}
                validationSchema={prompt.formValidationSchema}
              >
                {
                  <prompt.FormFields
                    onConfirm={handleConfirm}
                    onCancel={handleCancel}
                  />
                }
              </Form>
            )}
          </div>
        )}
      </Dialog>
    );
  }
);
