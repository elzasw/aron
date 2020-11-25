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

export function formFieldFactory<DATA_TYPE, PROPS>(
  Component: ComponentType<PROPS>,
  Wrapper: ComponentType<
    PropsWithChildren<FormFieldWrapperProps>
  > = FormFieldWrapper
) {
  return function FormField({
    name,
    label,
    notifyChange = noop,
    required = false,
    disabled = false,
    labelOptions = {},
    before,
    after,
    ...props
  }: FormFieldProps<PROPS>) {
    const [field, , helpers] = useField<DATA_TYPE | null | undefined>(name);

    const handleChange = useEventCallback((value: DATA_TYPE | null) => {
      helpers.setValue(value);

      // Do the notification on next frame, so the data in form are current
      requestAnimationFrame(() => notifyChange());
    });

    const { editing } = useContext(FormContext);

    return (
      <Wrapper
        label={label}
        required={required}
        disabled={!editing || disabled}
        labelOptions={labelOptions}
        before={before}
        after={after}
      >
        <Component
          name={name}
          {...(props as any)}
          disabled={!editing || disabled}
          value={field.value}
          onChange={handleChange}
        />
      </Wrapper>
    );
  };
}
