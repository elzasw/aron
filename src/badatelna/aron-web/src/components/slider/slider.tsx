import React, { ReactElement, useState, useEffect, useCallback } from 'react';
import MUISlider from '@material-ui/core/Slider';
import { isEqual } from 'lodash';

import { usePrevious } from '../../common-utils';

interface Props {
  onChange: (n: [number, number]) => void;
  interval: [number, number];
  defaultValue?: [number, number];
  controlledValue?: [number, number];
  className?: string;
  disabled?: boolean;
}

export function Slider({
  onChange,
  interval = [0, 100],
  defaultValue = interval,
  controlledValue,
  className,
  disabled,
}: Props): ReactElement {
  const createValue = useCallback(
    (newValue: [number, number]) =>
      newValue
        ? [
            Math.max(newValue[0], interval[0]),
            Math.min(newValue[1], interval[1]),
          ]
        : interval,
    [interval]
  );

  const prevValue = usePrevious(controlledValue);

  const [value, setValue] = useState<number[]>(createValue(defaultValue));

  const handleChange = (_: any, newValue: number | number[]) => {
    setValue(newValue as number[]);
  };

  const handleChangeCommitted = (_: any, newValue: number | number[]) => {
    setValue(newValue as number[]);
    onChange(newValue as [number, number]);
  };

  const marks = [
    { value: interval[0], label: interval[0].toString() },
    { value: interval[1], label: interval[1].toString() },
  ];

  useEffect(() => {
    if (controlledValue && !isEqual(prevValue, controlledValue)) {
      const newValue = createValue(controlledValue);

      if (value[0] !== newValue[0] || value[1] !== newValue[1]) {
        setValue(newValue);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [createValue, prevValue, controlledValue]);

  return (
    <MUISlider
      {...{
        value,
        onChange: handleChange,
        onChangeCommitted: handleChangeCommitted,
        className,
        marks,
        disabled,
        min: interval[0],
        max: interval[1],
        valueLabelDisplay: 'on',
      }}
    />
  );
}
