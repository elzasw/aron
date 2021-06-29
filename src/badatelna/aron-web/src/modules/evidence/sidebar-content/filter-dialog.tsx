import React, { forwardRef, useState, useEffect, useMemo } from 'react';
import classNames from 'classnames';
import { find, isEqual } from 'lodash';
import { FormattedMessage, useIntl } from 'react-intl';

import { DialogHandle, Dialog } from '@eas/common-web';

import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../../styles';
import {
  FilterComponent,
  createApiFilters,
  filterApiFilters,
  filterFacets,
  filterMappedFilters,
} from '../utils';
import { FilterConfig } from '../../../types';
import { Message } from '../../../enums';
import { FilterDialogProps } from '.';
import { usePrevious } from '../../../common-utils';
import { useFilters } from '../evidence-filters';
import { useMapFilters } from '../use-map-filters';

export const FilterDialog = forwardRef<DialogHandle, FilterDialogProps>(
  function FilterDialog(
    { filters: defaultFilters, onClose, apuPartItemTypes, facets, path },
    ref
  ) {
    const classes = useStyles();
    const layoutClasses = useLayoutStyles();
    const spacingClasses = useSpacingStyles();

    const { formatMessage } = useIntl();

    const { typeFilter, queryFilter, updateFilters } = useFilters();

    const [current, setCurrent] = useState<string>(defaultFilters[0]?.source);

    const prevFilters = usePrevious(defaultFilters);

    const [filters, setDialogFilters] = useState<FilterConfig[]>(
      defaultFilters
    );

    const mapFilters = useMapFilters();

    const apiFilters = useMemo(
      () => filterApiFilters([typeFilter, ...createApiFilters(filters)]),
      [typeFilter, filters]
    );

    const facetsOnPath = filterFacets(facets, path);

    const onFilterChange = (newFilter: FilterConfig) => {
      const setMappedFilters = async () => {
        const filterConfig = filters.map((f) =>
          f.source === newFilter.source ? { ...f, value: newFilter.value } : f
        );

        const mapped = await mapFilters(
          facetsOnPath,
          apuPartItemTypes,
          filterConfig,
          typeFilter,
          queryFilter
        );

        const filtered = filterMappedFilters(mapped, path);

        setDialogFilters(filtered);
      };

      setMappedFilters();
    };

    const handleConfirm = () => {
      updateFilters(filters);
      onClose();
    };

    const handleCancel = () => onClose();

    useEffect(() => {
      if (
        !isEqual(prevFilters, defaultFilters) &&
        !isEqual(defaultFilters, filters)
      ) {
        setDialogFilters(defaultFilters);
        setCurrent(defaultFilters[0].source);
      }
    }, [prevFilters, defaultFilters, filters]);

    const currentFilter = find(filters, (f) => f.source === current);

    return (
      <Dialog
        ref={ref}
        title={<FormattedMessage id={Message.ALL_FILTERS} />}
        onConfirm={handleConfirm}
        onCancel={handleCancel}
        confirmLabel={formatMessage({ id: Message.CONFIRM })}
        closeLabel={formatMessage({ id: Message.CANCEL })}
      >
        {() => (
          <div className={classNames(classes.filterDialog, layoutClasses.flex)}>
            <div className={classes.filterDialogLeft}>
              {filters.map((item) => (
                <div
                  key={item.source}
                  className={classNames(
                    classes.filterDialogItem,
                    item.source === current && classes.filterDialogItemActive
                  )}
                  onClick={() => setCurrent(item.source)}
                >
                  <div className={spacingClasses.padding}>{item.label}</div>
                </div>
              ))}
            </div>
            <div className={classes.filterDialogRight}>
              <div className={spacingClasses.padding}>
                <FilterComponent
                  {...{
                    ...currentFilter,
                    onChange: onFilterChange,
                    filters,
                    apiFilters,
                    inDialog: true,
                    apuPartItemTypes,
                  }}
                />
              </div>
            </div>
          </div>
        )}
      </Dialog>
    );
  }
);
