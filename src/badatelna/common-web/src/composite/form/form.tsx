import React, {
  useRef,
  useImperativeHandle,
  useMemo,
  memo,
  forwardRef,
  Ref,
  ReactElement,
  RefAttributes,
  ReactNode,
  useContext,
  useState,
} from 'react';
import { v4 as uuidv4 } from 'uuid';
import { reduce, merge, isFunction } from 'lodash';
import { Formik, FormikProps, yupToFormErrors } from 'formik';
import Grid from '@material-ui/core/Grid';
import Portal from '@material-ui/core/Portal';
import { FormProps, FormHandle, ValidationError } from './form-types';
import { useSubscriptionContext } from './selectors/selector';
import { Subscriber } from './selectors/subscriber';
import { SubscriptionContext } from './selectors/subscription-context';
import { FormContext } from './form-context';
import { useEventCallback } from 'utils/event-callback-hook';
import { useStyles } from './form-styles';
import { FormAnchorContext } from './form-anchor-context';

type ChildrenProps<DATA> =
  | ((props: FormikProps<DATA> & { formId: string }) => ReactNode)
  | ReactNode;

// eslint-disable-next-line react/display-name
export const Form = memo(
  forwardRef(function Form<DATA>(
    props: FormProps<DATA> & {
      children?: ChildrenProps<DATA>;
    },
    ref: Ref<FormHandle<DATA>>
  ) {
    const formId = useRef(uuidv4());

    const propsRef = useRef<FormikProps<DATA>>();
    const [errors, setErrors] = useState<ValidationError[]>([]);

    const { formAnchorRef } = useContext(FormAnchorContext);

    const validateForm = useEventCallback(async () => {
      const values = propsRef.current!.values;

      try {
        await props.validationSchema?.validate(values, {
          abortEarly: false,
          context: {
            values: values as any,
            // utils,
            // settings: appSettings,
          },
        });

        setErrors([]);
      } catch (e) {
        const yupErrors = yupToFormErrors<DATA>(e);
        const flattenedErrors = flattenObject(yupErrors);

        const errors = Object.keys(flattenedErrors).map((key) => {
          const value = flattenedErrors[key];

          return {
            key,
            value,
          } as ValidationError;
        });

        setErrors(errors);
        return errors;
      }

      return [];
    });

    const resetValidation = useEventCallback(() => {
      setErrors([]);
    });

    const context: FormContext<DATA> = useMemo(() => {
      return {
        formId: formId.current,
        editing: props.editing,
        errors,
        setErrors,
        getFieldValues: () => propsRef.current!.values,
        setFieldValues: (values) => propsRef.current!.setValues(values),
        setFieldValue: (name, value) =>
          propsRef.current!.setFieldValue(name, value),
        submitForm: () => propsRef.current!.submitForm(),
        clearForm: () => propsRef.current?.resetForm(),
        validateForm,
        resetValidation,
      };
    }, [props.editing, validateForm, resetValidation, errors]);

    useImperativeHandle(ref, () => context, [context]);

    const { subscriptionContext, fireSubscriptions } = useSubscriptionContext(
      context.getFieldValues
    );

    const classes = useStyles();

    return (
      <Formik initialValues={props.initialValues} onSubmit={props.onSubmit}>
        {(formikProps: FormikProps<DATA>) => {
          propsRef.current = formikProps;

          return (
            <FormContext.Provider value={context}>
              <SubscriptionContext.Provider value={subscriptionContext}>
                <Subscriber fireSubscriptions={fireSubscriptions} />
                {/* To prevent validateDOMNesting warning: <form> cannot appear as a descendant of <form> */}
                <Portal container={formAnchorRef.current}>
                  <form
                    id={formId.current}
                    action={props.action}
                    method={props.method}
                    onReset={formikProps.handleReset}
                    onSubmit={
                      props.browserSubmit ? undefined : formikProps.handleSubmit
                    }
                  />
                </Portal>
                <div className={classes.form}>
                  <Grid container spacing={0}>
                    {isFunction(props.children)
                      ? props.children({
                          ...formikProps,
                          formId: formId.current,
                        })
                      : props.children}
                  </Grid>
                </div>
              </SubscriptionContext.Provider>
            </FormContext.Provider>
          );
        }}
      </Formik>
    );
  })
) as <DATA>(
  p: FormProps<DATA> & {
    children?: ChildrenProps<DATA>;
  } & RefAttributes<FormHandle<DATA>>
) => ReactElement;

/**
 * Deep flatten object, creating chained keys with dot.
 *
 * @param obj Object to flatten
 * @param path Current path in recursion
 */
function flattenObject(
  obj: Record<string, any>,
  path: string[] = []
): Record<string, string> {
  if (typeof obj === 'string') {
    return { [path.join('.')]: obj };
  } else {
    return reduce(
      obj,
      (cum, next, key) => merge(cum, flattenObject(next, [...path, key])),
      {}
    );
  }
}
