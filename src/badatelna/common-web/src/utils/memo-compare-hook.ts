import { useRef, useEffect } from 'react';

export function useMemoCompare<ObjectType>(
  next: ObjectType,
  compare: (prev?: ObjectType, next?: ObjectType) => boolean
) {
  // Ref for storing previous value
  const previousRef = useRef<ObjectType>();
  const previous = previousRef.current;

  // Pass previous and next value to compare function
  // to determine whether to consider them equal.
  const isEqual = compare(previous, next);

  // If not equal update previousRef to next value.
  // We only update if not equal so that this hook continues to return
  // the same old value if compare keeps returning true.
  useEffect(() => {
    if (!isEqual) {
      previousRef.current = next;
    }
  });

  // Finally, if equal then return the previous value
  return isEqual ? previous : next;
}
