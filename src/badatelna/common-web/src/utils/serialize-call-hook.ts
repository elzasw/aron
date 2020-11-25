import { useRef } from 'react';
import { Deferred } from './deferred';
import { useEventCallback } from './event-callback-hook';

export type Call = Deferred<void>;

/**
 * Call serializer hook.
 *
 * Provides the caller with methods to enqueue and finish async calls.
 * Useful when the calls are depending on the state change from the previous one.
 *
 *
 * @param onCall onCall callback
 */
export function useSerializeCall(onCall: () => Promise<void>) {
  const callsQueue = useRef<Call[]>([]);

  /**
   * Adds calls to queue.
   */
  const enqueueCall = useEventCallback(async function () {
    // Enqueues new call
    const call = new Deferred<void>();
    callsQueue.current.push(call);

    if (callsQueue.current.length === 1) {
      // if there is only this one call enqueued, start processing

      while (callsQueue.current.length > 0) {
        // processes every call one by one removing it from the queue
        // we can not call callsQueue.current.pop() because new call can be enqueued during onCall async processing,
        // therefore we will call callsQueue.current.shift() to remove processed call from queue only after the processing is completed
        const call = callsQueue.current[0];

        try {
          await onCall();
          call.resolve();
          callsQueue.current.shift();
        } catch (err) {
          // rejects all outstanding calls with the same error
          callsQueue.current.forEach((call) => call.reject(err));
        }
      }
    }

    return call.promise;
  });

  return [enqueueCall] as [() => Promise<void>];
}
