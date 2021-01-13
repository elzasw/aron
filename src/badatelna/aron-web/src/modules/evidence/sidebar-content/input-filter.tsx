import React, { ReactElement, useState, useCallback } from 'react';

import {
  Option,
  ApiFilterOperation,
  Filter,
  AggregationItem,
} from '../../../types';
import { FilterObject, FilterChangeCallBack } from '../types';
import { Autocomplete, TextField } from '../../../components';
import { isEmpty, debounce, get, isArray } from 'lodash';
import { useStyles } from './styles';
import { FilterType } from '../../../enums';
import { useGetOptionsByField } from '../../../common-utils';

interface Props {
  field: string;
  label: string;
  onChange: FilterChangeCallBack;
  value: Option;
  operation?: ApiFilterOperation;
  filters?: Filter[];
  autocomplete?: boolean;
  multiple?: boolean;
}

export default function InputFilter({
  field,
  label,
  onChange,
  value,
  operation = ApiFilterOperation.EQ,
  filters,
  autocomplete = true,
  multiple = false,
}: Props): ReactElement {
  const [inputValue, setInputValue] = useState<Option[] | Option | null>(value);

  const [query, setQuery] = useState('');

  const [result, loading] = useGetOptionsByField(query, field);
  const updateFilterValue = useCallback(
    (newFilterValue: FilterObject | null) => {
      onChange(
        field,
        !isEmpty(newFilterValue) ? newFilterValue : null,
        operation,
        filters,
        FilterType.INPUT
      );
    },

    [field, onChange, operation, filters]
  );
  const handleAutocompleteInputChange = (options: Option[] | Option | null) => {
    setInputValue(options);
    updateFilterValue(
      isEmpty(options)
        ? {}
        : isArray(options)
        ? options.reduce(
            (result: FilterObject, option: Option) => ({
              ...result,
              [option.id]: option.id,
            }),
            {}
          )
        : options === null
        ? {}
        : { [options.id]: options.id }
    );
  };

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const updateFilterDebounced = useCallback(debounce(updateFilterValue, 500), [
    updateFilterValue,
  ]);

  const handleTextFieldInputChange = (name: any) => {
    const newOptionObject = name.length > 0 ? { id: '_TEXTFIELD', name } : null;
    setInputValue(newOptionObject);
    name.length > 0 ? updateFilterDebounced([name]) : updateFilterValue(null);
  };

  const classes = useStyles();
  return (
    <>
      <div className={classes.filterTitle}>{label}</div>
      {autocomplete ? (
        <Autocomplete
          {...{
            options: get(result, 'aggregations.items', []).map(
              (option: AggregationItem) => ({
                id: option.key,
                name: `${option[`${field}~LABEL`]} (${option.value})`,
              })
            ),
            onQueryChange: setQuery,
            value: inputValue,
            onChange: handleAutocompleteInputChange,
            multiple,
            loading,
          }}
        />
      ) : (
        //TODO if needed rework to multi, now TextField works just with single values
        <TextField
          className={classes.inputFilterTextField}
          {...{
            size: 'small',
            value:
              (!isArray(inputValue) && inputValue && inputValue.name) || '',
            onChange: handleTextFieldInputChange,
          }}
        />
      )}
    </>
  );
}
