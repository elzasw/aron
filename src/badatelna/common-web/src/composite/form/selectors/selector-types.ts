/**
 * Selector function to return only part of the provided object.
 */
export type FormSelector<T, U> = (values: T) => U;

/**
 * Comparator function to compare two values and returns if they are the same.
 */
export type FormComparator<U> = (newValue: U, prevValue: U) => boolean;


