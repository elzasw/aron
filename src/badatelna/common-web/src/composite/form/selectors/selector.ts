import { useContext, useRef, useEffect, useMemo } from 'react';
import { isEqual } from 'lodash';
import { useEventCallback } from 'utils/event-callback-hook';
import { FormSelector, FormComparator } from './selector-types';
import { SelectorContext } from './selector-context';
import { SubscriptionContext, FormSubscription } from './subscription-context';
import { useForceRender } from 'utils/force-render';

/**
 * Gets value from form at selected place.
 *
 * fixme: add support for FormFieldContext.
 *
 * @param selector Selector function accepting the form object and returning the value
 * @param providedContext Custom context for accessing parent form
 * @param comparator Comparator function to detect value change. Defaults to lodash deep compare
 */
export function useFormSelector<T, U>(
  selector: FormSelector<T, U>,
  providedContext?: SubscriptionContext<T>,
  comparator: FormComparator<U> = isEqual
) {
  const { selector: rootSelector = (values: T) => values } = useContext(
    SelectorContext
  );
  const combinedSelector = (values: T) => selector(rootSelector(values) ?? {});

  const defaultContext = useContext<SubscriptionContext<T>>(
    SubscriptionContext
  );
  const context = providedContext ?? defaultContext;

  const { forceRender } = useForceRender();
  const valueRef = useRef(context.getValue(combinedSelector));

  useEffect(() => {
    // creates subscription
    const checkForUpdates = () => {
      const newValue = context.getValue(combinedSelector);

      if (!comparator(newValue, valueRef.current)) {
        valueRef.current = newValue;
        forceRender();
      }
    };

    context.subscriptions.add(checkForUpdates);

    // removes subscription
    return () => {
      context.subscriptions.delete(checkForUpdates);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [context]);

  return valueRef.current;
}

/**
 * Creates new form subscription context.
 *
 */
export function useSubscriptionContext<DATA>(getValues: () => DATA) {
  /**
   * Form values.
   */

  /**
   * Subscriptons is mutable set, therefore use Ref instead of State.
   */
  const subscriptions = useRef<Set<FormSubscription>>(new Set());

  /**
   * Calls all subscribed handlers.
   */
  const fireSubscriptions = useEventCallback(() => {
    subscriptions.current.forEach((subscription) => subscription());
  });

  const parent = useContext(SubscriptionContext);

  /**
   * Context object needs to be memoized.
   */
  const subscriptionContext: SubscriptionContext<DATA> = useMemo(
    () => ({
      subscriptions: subscriptions.current,
      getValue: (selector: FormSelector<DATA, any>) => selector(getValues()),
      parent,
    }),
    [getValues, parent]
  );

  return { subscriptionContext, fireSubscriptions };
}
