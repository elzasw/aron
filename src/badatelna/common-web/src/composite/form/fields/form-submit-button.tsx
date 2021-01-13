import React, { useContext } from 'react';
import Button, { ButtonProps } from '@material-ui/core/Button';
import { FormContext } from '../form-context';

export function FormSubmitButton(props: ButtonProps) {
  const { formId } = useContext(FormContext);

  return <Button {...props} form={formId} />;
}
