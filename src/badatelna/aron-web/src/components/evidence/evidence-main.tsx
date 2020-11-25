import React, { useState, useCallback } from 'react';
import classNames from 'classnames';
import { get, find, isEqual } from 'lodash';

import { EvidenceSidebar } from './evidence-sidebar';
import { EvidenceList } from './evidence-list';
import { MainProps, FiltersChangeCallbackParams } from './types';
import { ApiUrl, ModulePath } from '../../enums';
import { useStyles } from './styles';
import { useLayoutStyles } from '../../styles';
import { useApiList } from '../../hooks';
import { filtersData } from '../../enums';
import { EvidenceWrapper } from './evidence-wrapper';
import { ApiFilterOperation, Filter } from '../../types';
import { getTypeByPath } from './utils';

export function EvidenceMain({ modulePath: path, label }: MainProps) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();

  const urlParams = new URLSearchParams(window.location.search);
  const query = urlParams.get('query');

  const type = getTypeByPath(path);

  const [filters, setFilters] = useState([
    ...(type
      ? [{ field: 'type', operation: ApiFilterOperation.EQ, value: type }]
      : []),
    { operation: ApiFilterOperation.FTX, value: query },
  ] as Filter[]);

  const [result, loading] = useApiList(ApiUrl.APU, {
    json: {
      filters: filters.filter(
        ({ value, operation }) =>
          value ||
          operation === ApiFilterOperation.OR ||
          operation === ApiFilterOperation.AND
      ),
    },
  });

  const handleChange = useCallback(
    ({ query, filters: newFilters }: FiltersChangeCallbackParams) => {
      const currentQuery = get(
        find(filters, (f) => f.operation === ApiFilterOperation.FTX),
        'value'
      );

      if (query !== currentQuery && (query || currentQuery)) {
        setFilters(
          filters.map((f) =>
            f.operation === ApiFilterOperation.FTX ? { ...f, value: query } : f
          )
        );
      } else {
        const allFilters = [
          ...filters.filter(
            ({ field, operation }) =>
              field === 'type' || operation === ApiFilterOperation.FTX
          ),
          ...(newFilters || []),
        ];

        if (!isEqual(filters, allFilters)) {
          setFilters(allFilters);
        }
      }
    },
    [filters, setFilters]
  );

  console.log('loading :>> ', loading);

  return (
    <EvidenceWrapper
      {...{
        items: [{ label }],
      }}
    >
      <div className={classNames(classes.evidenceMain, layoutClasses.flex)}>
        <EvidenceSidebar
          path={path}
          query={query || ''}
          filters={filtersData[path as ModulePath] || []}
          onChange={handleChange}
        />
        <EvidenceList
          {...{
            items: get(result, 'items', []),
          }}
        />
      </div>
    </EvidenceWrapper>
  );
}
