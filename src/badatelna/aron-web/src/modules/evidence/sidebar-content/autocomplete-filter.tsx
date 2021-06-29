import React, { ReactElement, useState, useCallback } from 'react';
import { isArray, debounce } from 'lodash';

import { Option, AggregationItem } from '../../../types';
import { Autocomplete, Tooltip } from '../../../components';
import { useStyles } from './styles';
import {
  useGetOptionsBySource,
  getApuPartItemType,
  parseApuRefOptionId,
  parseApuRefOptionLabel,
} from '../../../common-utils';
import { AutocompleteFilterProps } from './types';
import { ApuPartItemDataType } from '../../../enums';

export function AutocompleteFilter({
  value,
  onChange,
  source,
  label,
  tooltip,
  description,
  inDialog,
  apuPartItemTypes,
  apiFilters = [],
}: AutocompleteFilterProps): ReactElement {
  const classes = useStyles();

  const [apiQuery, setApiQuery] = useState('');
  const [query, setQuery] = useState('');

  const updateApiQuery = debounce(
    (apiQuery: string) => setApiQuery(apiQuery),
    500
  );

  const isApuRef =
    getApuPartItemType(apuPartItemTypes, source) ===
    ApuPartItemDataType.APU_REF;

  const [result, loading] = useGetOptionsBySource(
    source,
    apiQuery,
    isApuRef,
    apiFilters
  );

  const updateFilterValue = useCallback(
    (newFilterValue: Option | Option[] | null) => {
      onChange({
        source,
        value: newFilterValue
          ? isArray(newFilterValue)
            ? newFilterValue
            : [newFilterValue]
          : [],
      });
    },

    [onChange, source]
  );

  const allOptions = (result || []).map((option: AggregationItem) => ({
    id: isApuRef ? parseApuRefOptionId(option.key) : option.key,
    name: isApuRef ? parseApuRefOptionLabel(option.key) : option.key,
    value: option.value,
  }));

  const options = allOptions
    .filter(({ name }: { name: string }) => !query || name.indexOf(query) >= 0)
    .map((option: { id: string; name: string; value: string }) => ({
      id: option.id,
      name: `${option.name} (${option.value})`,
    }));

  return (
    <div className={classes.autocompleteFilter}>
      <div className={classes.filterTitle}>
        <Tooltip title={tooltip}>
          <div>{label}</div>
        </Tooltip>
      </div>
      {inDialog && description ? (
        <div className={classes.filterDescription}>{description}</div>
      ) : (
        <></>
      )}
      <Autocomplete
        {...{
          options,
          onQueryChange: (t: any) => {
            setQuery(t);
            updateApiQuery(t);
          },
          value,
          onChange: updateFilterValue,
          multiple: true,
          loading,
        }}
      />
    </div>
  );
}
