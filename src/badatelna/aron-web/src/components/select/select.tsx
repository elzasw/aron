import React from 'react';

import { Select as EasSelect } from '@eas/common-web';

import { Option } from '../../types';

interface SelectProps {
  value: Option;
  options: Option[];
  onChange: (value: any) => void;
  loading?: boolean;
  clearable?: boolean;
}

export const Select: React.FC<SelectProps> = ({
  value,
  options,
  onChange,
  loading = false,
  clearable = true,
}) => {
  return (
    <EasSelect
      value={value}
      source={{
        items: options,
        loading,
        reset: () => null,
        loadDetail: (o: any) => o,
      }}
      onChange={onChange}
      {...{ clearable }}
    />
  );
};
