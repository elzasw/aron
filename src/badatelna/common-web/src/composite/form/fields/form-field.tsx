import React, { ComponentType, PropsWithChildren, useContext } from 'react';
import { noop } from 'lodash';
import { useField } from 'formik';
import { useEventCallback } from 'utils/event-callback-hook';
import { FormFieldWrapper } from './wrapper/form-field-wraper';
import {
  FormFieldProps,
  FormFieldWrapperProps,
} from './wrapper/form-field-wrapper-types';
import { FormContext } from '../form-context';
import { FormFieldContext } from './form-field-context';

export function formFieldFactory<DATA_TYPE, PROPS>(
  Component: ComponentType<PROPS>,
  Wrapper: ComponentType<
    PropsWithChildren<FormFieldWrapperProps>
  > = FormFieldWrapper
) {
  return function FormField({
    name: providedName,
    label,
    helpLabel,
    notifyChange = noop,
    required = false,
    disabled = false,
    labelOptions = {},
    layoutOptions = {},
    errorOptions = {},
    before,
    after,
    ...props
  }: FormFieldProps<PROPS>) {
    const formFieldContext = useContext(FormFieldContext);

    const name =
      formFieldContext.prefix !== undefined
        ? `${formFieldContext.prefix}.${providedName}`
        : providedName;

    const [field, , helpers] = useField<DATA_TYPE | null | undefined>(name);

    const handleChange = useEventCallback((value: DATA_TYPE | null) => {
      helpers.setValue(value);

      // Do the notification on next frame, so the data in form are current
      requestAnimationFrame(() => notifyChange());
    });

    const { editing, formId, errors } = useContext(FormContext);

    const fieldErrors = errors.filter((e) => e.key === name);

    return (
      <Wrapper
        label={label}
        helpLabel={helpLabel}
        required={formFieldContext.required ?? required}
        disabled={formFieldContext.disabled ?? (!editing || disabled)}
        labelOptions={labelOptions}
        layoutOptions={layoutOptions}
        errorOptions={errorOptions}
        before={before}
        after={after}
        errors={fieldErrors}
      >
        <Component
          name={name}
          {...(props as any)}
          form={formId}
          disabled={formFieldContext.disabled ?? (!editing || disabled)}
          value={field.value}
          onChange={handleChange}
        />
      </Wrapper>
    );
  };
}
