import React, { useState, useRef, useContext, useCallback } from 'react';
import classNames from 'classnames';
import { map, isNumber, get, find, cloneDeep } from 'lodash';
import IconButton from '@material-ui/core/IconButton';
import ChevronRight from '@material-ui/icons/ChevronRight';

import {
  DialogHandle,
  useEventCallback,
  NavigationContext,
} from '@eas/common-web';

import { Search } from '../search';
import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import { FilterDialog } from './sidebar-content/filter-dialog';
import {
  SidebarProps,
  FiltersChangeCallbackParams,
  FilterObject,
  FilterData,
} from './types';
import { getFilterComponent } from './utils';
import { Button } from '../button';
import { getUrlWithQuery } from '../../common-utils';
import { ApiFilterOperation } from '../../types';

export const EvidenceSidebar: React.FC<SidebarProps> = ({
  path,
  query,
  filters,
  onChange,
}) => {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const [visible, setVisible] = useState(true);
  const [dialogKey, setDialogKey] = useState(true);
  const [filterData, setFilterData] = useState<FilterData[]>([]);

  const dialogRef = useRef<DialogHandle>(null);

  const handleAllFilters = useEventCallback(() => {
    dialogRef.current?.open();
  });

  const { navigate } = useContext(NavigationContext);

  const onChangeHandler = useCallback(
    (params: FiltersChangeCallbackParams) => {
      if (params.query) {
        navigate(getUrlWithQuery(path, params.query));
      }
      onChange(params);
    },
    [onChange, navigate, path]
  );

  const updateFilters = useCallback(
    (newFilterData: FilterData[]) => {
      setFilterData(newFilterData);
      onChangeHandler({
        filters: newFilterData.length
          ? newFilterData.map(({ name, filterObject }) => {
              const options: string[] = [];

              map(filterObject, (value, key) => {
                if (isNumber(Number(key))) {
                  options.push(value);
                }
              });

              return {
                operation: ApiFilterOperation.OR,
                filters: options.map((value) => ({
                  field: name,
                  operation: ApiFilterOperation.EQ,
                  value,
                })),
              };
            })
          : [],
      });
    },
    [onChangeHandler]
  );

  const onFilterChange = useCallback(
    (name: string, filterObject: FilterObject | null) => {
      updateFilters([
        ...filterData.filter((f) => f.name !== name),
        ...(filterObject ? [{ name, filterObject }] : []),
      ]);
    },
    [filterData, updateFilters]
  );

  const handleDialogClose = () => {
    setTimeout(() => setDialogKey(!dialogKey));
  };

  return (
    <div
      className={classNames(classes.sidebar, visible && classes.sidebarVisible)}
    >
      <div
        className={classNames(
          classes.visibleButton,
          layoutClasses.flexCentered,
          spacingClasses.paddingVerticalSmall,
          { [layoutClasses.flexEnd]: visible }
        )}
        onClick={() => setVisible(!visible)}
      >
        <IconButton color="primary">
          <ChevronRight
            className={
              visible
                ? classes.toggleSidebarButtonOpen
                : classes.toggleSidebarButtonClose
            }
          />
        </IconButton>
      </div>
      <div
        className={classNames(classes.sidebarContent, {
          [classes.sidebarContentVisible]: visible,
        })}
      >
        <div className={spacingClasses.padding}>
          <div className={spacingClasses.paddingVertical}>
            <Search
              value={query}
              onSearch={({ query }) => onChangeHandler({ query })}
            />
          </div>
          {filters.map((f, index) =>
            getFilterComponent({
              ...f,
              index,
              onChange: onFilterChange,
              value: get(
                find(filterData, ({ name }) => name === f.name),
                'filterObject'
              ),
            })
          )}
          {filters.length ? (
            <div className={spacingClasses.paddingTop}>
              <Button
                contained={true}
                label="Všechny filtry"
                color="primary"
                onClick={handleAllFilters}
              />
              <FilterDialog
                ref={dialogRef}
                key={dialogKey}
                filters={filters}
                filterData={cloneDeep(filterData)}
                onConfirm={updateFilters}
                onCancel={handleDialogClose}
              />
            </div>
          ) : (
            <></>
          )}
        </div>
      </div>
    </div>
  );
};
