import React, {
  ComponentType,
  PropsWithChildren,
  useContext,
  useMemo,
} from 'react';
import { noop, isEqual } from 'lodash';
import { useField } from 'formik';
import { useEventCallback } from 'utils/event-callback-hook';
import { FormFieldWrapper } from './wrapper/form-field-wraper';
import {
  FormFieldProps,
  FormFieldWrapperProps,
} from './wrapper/form-field-wrapper-types';
import { FormContext } from '../form-context';
import { FormFieldContext } from './form-field-context';
import { useMemoCompare } from '../../../utils/memo-compare-hook';

export function formFieldFactory<DATA_TYPE, PROPS>(
  Component: ComponentType<PROPS>,
  Wrapper: ComponentType<
    PropsWithChildren<FormFieldWrapperProps>
  > = FormFieldWrapper
) {
  const MemoizedComponent = React.memo(Component);
  return function FormField({
    name: providedName,
    label,
    helpLabel,
    notifyChange = noop,
    required = false,
    disabled = false,
    before,
    after,
    ...props
  }: FormFieldProps<PROPS>) {
    const formFieldContext = useContext(FormFieldContext);

    const { errorOptions, labelOptions, layoutOptions } = {
      labelOptions: useMemo(() => ({}), []),
      layoutOptions: useMemo(() => ({}), []),
      errorOptions: useMemo(() => ({}), []),
      ...props,
    };

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

    const fieldErrors = useMemo(() => errors.filter((e) => e.key === name), [
      errors,
      name,
    ]);

    const memoizedProps = useMemoCompare(props, isEqual);

    return useMemo(
      () => (
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
          <MemoizedComponent
            name={name}
            {...(memoizedProps as any)}
            form={formId}
            disabled={formFieldContext.disabled ?? (!editing || disabled)}
            value={field.value}
            onChange={handleChange}
          />
        </Wrapper>
      ),
      [
        label,
        helpLabel,
        formFieldContext,
        required,
        disabled,
        editing,
        labelOptions,
        layoutOptions,
        errorOptions,
        before,
        after,
        fieldErrors,
        name,
        formId,
        field.value,
        handleChange,
        memoizedProps,
      ]
    );
  };
}
