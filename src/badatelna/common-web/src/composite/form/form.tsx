import React, {
  PropsWithChildren,
  useRef,
  useImperativeHandle,
  useMemo,
  memo,
  forwardRef,
  Ref,
  ReactElement,
  RefAttributes,
} from 'react';
import { reduce, merge } from 'lodash';
import { Formik, FormikProps, yupToFormErrors } from 'formik';
import Grid from '@material-ui/core/Grid';
import { FormProps, FormHandle, ValidationError } from './form-types';
import { useSubscriptionContext } from './selectors/selector';
import { Subscriber } from './selectors/subscriber';
import { SubscriptionContext } from './selectors/subscription-context';
import { FormContext } from './form-context';
import { useEventCallback } from 'utils/event-callback-hook';
import { useStyles } from './form-styles';

// eslint-disable-next-line react/display-name
export const Form = memo(
  forwardRef(function Form<DATA>(
    props: PropsWithChildren<FormProps<DATA>>,
    ref: Ref<FormHandle<DATA>>
  ) {
    const propsRef = useRef<FormikProps<DATA>>();

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
      } catch (e) {
        const yupErrors = yupToFormErrors<DATA>(e);
        const flattenedErrors = flattenObject(yupErrors);

        return Object.keys(flattenedErrors).map((key) => {
          const value = flattenedErrors[key];
          const parsed = value.split('@@@', 3);

          return {
            key,
            label: parsed[0],
            value: parsed[1],
          } as ValidationError;
        });
      }

      return [];
    });

    const context: FormContext<DATA> = useMemo(() => {
      return {
        editing: props.editing,
        getFieldValues: () => propsRef.current!.values,
        setFieldValues: (values) => propsRef.current!.setValues(values),
        setFieldValue: (name, value) =>
          propsRef.current!.setFieldValue(name, value),
        submitForm: () => propsRef.current!.submitForm(),
        clearForm: () => propsRef.current?.resetForm(),
        validateForm,
      };
    }, [props.editing, validateForm]);

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
                <form
                  className={classes.form}
                  action={props.action}
                  method={props.method}
                  onReset={formikProps.handleReset}
                  onSubmit={
                    props.browserSubmit ? undefined : formikProps.handleSubmit
                  }
                >
                  <Grid container spacing={0}>
                    {props.children}
                  </Grid>
                </form>
              </SubscriptionContext.Provider>
            </FormContext.Provider>
          );
        }}
      </Formik>
    );
  })
) as <DATA>(
  p: PropsWithChildren<FormProps<DATA>> & RefAttributes<FormHandle<DATA>>
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
