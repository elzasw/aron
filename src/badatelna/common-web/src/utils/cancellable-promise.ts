import { useRef, useCallback } from 'react';

export function cancellablePromise<T>(promise: Promise<T>) {
  let isCanceled = false;

  const wrappedPromise = new Promise<T>((resolve, reject) => {
    promise.then(
      (value) => (isCanceled ? reject({ isCanceled, value }) : resolve(value)),
      (error) => reject({ isCanceled, error })
    );
  });

  return {
    promise: wrappedPromise,
    cancel: () => (isCanceled = true),
  };
}

type CancellablePromise<T> = { promise: Promise<T>; cancel: () => boolean };

export function useCancellablePromises<T>() {
  const pendingPromises = useRef([] as CancellablePromise<T>[]);

  const appendPendingPromise = useCallback(function (
    promise: CancellablePromise<T>
  ) {
    pendingPromises.current = [...pendingPromises.current, promise];
  },
  []);

  const removePendingPromise = useCallback(function (
    promise: CancellablePromise<T>
  ) {
    pendingPromises.current = pendingPromises.current.filter(
      (p) => p !== promise
    );
  },
  []);

  const clearPendingPromises = useCallback(function () {
    pendingPromises.current.map((p) => p.cancel());
  }, []);

  return [appendPendingPromise, removePendingPromise, clearPendingPromises] as [
    typeof appendPendingPromise,
    typeof removePendingPromise,
    typeof clearPendingPromises
  ];
}
