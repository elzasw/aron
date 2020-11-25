import React from 'react';
import { FilterComponentProps } from '../table-types';
import { Select } from 'components/select/select';
import { useIntl } from 'react-intl';
import { useStaticListSource } from 'utils/list-source-hook';

export function FilterBooleanCell({
  disabled,
  value,
  onChange,
}: FilterComponentProps) {
  const intl = useIntl();

  return (
    <Select<{ id: string; name: string }>
      disabled={disabled}
      source={useStaticListSource([
        {
          id: 'true',
          name: intl.formatMessage({
            id: 'EAS_TABLE_FILTER_CELL_BOOLEAN_OPTION_SELECTED',
            defaultMessage: 'Vybrané',
          }),
        },
        {
          id: 'false',
          name: intl.formatMessage({
            id: 'EAS_TABLE_FILTER_CELL_BOOLEAN_OPTION_NOT_SELECTED',
            defaultMessage: 'Nevybrané',
          }),
        },
      ])}
      value={value}
      onChange={onChange}
      valueIsId={true}
    />
  );
}
