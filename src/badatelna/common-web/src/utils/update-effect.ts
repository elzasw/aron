import { EffectCallback, DependencyList, useRef, useEffect } from 'react';

export function useUpdateEffect(effect: EffectCallback, deps?: DependencyList) {
  const mounted = useRef(false);

  useEffect(() => {
    if (mounted.current === false) {
      mounted.current = true;
    } else {
      return effect();
    }
    /* eslint-disable react-hooks/exhaustive-deps */
  }, [...(deps ?? [])]);
}
