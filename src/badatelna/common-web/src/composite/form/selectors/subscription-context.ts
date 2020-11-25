import { createContext } from 'react';
import { FormSelector } from './selector-types';

/**
 * Internal subscription, which is only a method to call.
 */
export type FormSubscription = () => void;

/**
 * Formik subscription context type.
 */
export interface SubscriptionContext<T> {
  subscriptions: Set<FormSubscription>;
  getValue: <U>(selector: FormSelector<T, U>) => U;

  parent: SubscriptionContext<T>;
}


/**
 * Form subscription context.
 */
export const SubscriptionContext = createContext<
  SubscriptionContext<any>
>(undefined as never);