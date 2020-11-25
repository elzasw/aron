import { useState, useEffect } from 'react';
import { useEventCallback } from './event-callback-hook';

type State<TYPE> = { value: TYPE; onChange: (value: TYPE) => void };

export function useInternalState<TYPE>(
  external: State<TYPE>,
  inCondition: (value: TYPE, extValue: TYPE) => boolean,
  outCondition: (value: TYPE) => boolean
) {
  const [value, setValue] = useState(external.value);
  const [synced, setSynced] = useState(true);

  useEffect(() => {
    if (inCondition(value, external.value)) {
      setValue(external.value);
      setSynced(true);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [external.value, inCondition]);

  const onChange = useEventCallback((value: TYPE) => {
    setValue(value);

    if (outCondition(value)) {
      external.onChange(value);
      setSynced(true);
    } else {
      setSynced(false);
    }
  });
  return [value, onChange, synced] as [TYPE, (value: TYPE) => void, boolean];
}
