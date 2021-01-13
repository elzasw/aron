import React, {
  useState,
  useRef,
  useContext,
  useCallback,
  useEffect,
} from 'react';
import classNames from 'classnames';
import {
  map,
  isNumber,
  get,
  find,
  cloneDeep,
  isEmpty,
  compact,
  isEqual,
} from 'lodash';
import IconButton from '@material-ui/core/IconButton';
import ChevronRight from '@material-ui/icons/ChevronRight';
import { useIntl } from 'react-intl';

import {
  DialogHandle,
  useEventCallback,
  NavigationContext,
} from '@eas/common-web';

import { Button, Search } from '../../components';
import { useStyles } from './styles';
import { useSpacingStyles } from '../../styles';
import { FilterDialog } from './sidebar-content/filter-dialog';
import {
  SidebarProps,
  FiltersChangeCallbackParams,
  FilterObject,
  FilterData,
} from './types';
import { getFilterComponent, convertRelationshipsToFilter } from './utils';
import { getUrlWithQuery } from '../../common-utils';
import { ApiFilterOperation, Filter, Relationship } from '../../types';
import { FilterType, Message } from '../../enums';
import RelationshipFilter from './sidebar-content/relationship-filter';

export const EvidenceSidebar: React.FC<SidebarProps> = ({
  path,
  query,
  filters,
  relationships,
  setRelationships,
  onChange,
  apuPartItemTypes,
}) => {
  const classes = useStyles();
  const spacingClasses = useSpacingStyles();

  const { formatMessage } = useIntl();

  const [visible, setVisible] = useState(false);
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
      onChange({ query, ...params });
    },
    // eslint-disable-next-line
    [onChange, navigate, path]
  );

  const processFilterData = (newFilterData: FilterData[]) => ({
    filters: newFilterData.length
      ? compact(
          newFilterData.map(
            ({ field, filterObject, operation, filters, type }) => {
              const options: any[] = [];

              map(filterObject, (value, key) => {
                if (isNumber(Number(key))) {
                  options.push(value);
                }
              });

              return options && options.length
                ? {
                    operation: ApiFilterOperation.OR,
                    filters: options.map((value) => {
                      if (type === FilterType.CHECKBOX_WITH_RANGE) {
                        return {
                          operation: ApiFilterOperation.AND,
                          filters: [
                            {
                              field,
                              operation: ApiFilterOperation.GTE,
                              value: value.from,
                            },
                            {
                              field,
                              operation: ApiFilterOperation.LT,
                              value: value.to,
                            },
                          ],
                        };
                      }
                      return {
                        field,
                        operation: operation || ApiFilterOperation.EQ,
                        value,
                        ...(filters ? { filters: filters } : {}),
                      };
                    }),
                  }
                : null;
            }
          )
        )
      : [],
  });
  const updateFiltersAndRelationships = useCallback(
    (newFilterData: FilterData[], newRelationships: Relationship[] | null) => {
      const relationshipsFilter =
        newRelationships && convertRelationshipsToFilter(newRelationships);
      if (!isEqual(newFilterData, filterData)) setFilterData(newFilterData);
      if (!isEqual(newRelationships, relationships))
        setRelationships(newRelationships);
      const filters: Filter[] = [
        ...processFilterData(newFilterData).filters,
        ...(relationshipsFilter ? [relationshipsFilter] : []),
      ];
      onChangeHandler({ filters });
    },
    [filterData, onChangeHandler, relationships, setRelationships]
  );

  const onFilterChange = useCallback(
    (
      field: string,
      filterObject: FilterObject | null,
      operation: ApiFilterOperation,
      filters: Filter[],
      type: FilterType
    ) => {
      const newFilterData = [
        ...filterData.filter((f) => f.field !== field),
        ...(filterObject && !isEmpty(filterObject)
          ? [{ field, filterObject, operation, filters, type }]
          : []),
      ];
      updateFiltersAndRelationships(newFilterData, relationships);
    },
    [filterData, relationships, updateFiltersAndRelationships]
  );
  const onRelationshipsChange = useCallback(
    (newRelationships: Relationship[]) =>
      updateFiltersAndRelationships(filterData, newRelationships),
    [filterData, updateFiltersAndRelationships]
  );
  useEffect(() => {
    //if there are initial relationships
    const relationshipFilter =
      relationships && convertRelationshipsToFilter(relationships);
    const allInitial = relationshipFilter ? [relationshipFilter] : [];

    //if any of the filters has set initial value in filterObject
    if (
      !filters.reduce(
        (allEmpty, filter) => allEmpty && isEmpty(filter.filterObject),
        true
      )
    ) {
      allInitial.push(...processFilterData(filters).filters);
    }
    onChangeHandler({ filters: allInitial });
    // eslint-disable-next-line
  }, [filters]);

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
          spacingClasses.paddingVerticalSmall
        )}
        onClick={() => setVisible(!visible)}
      >
        <IconButton size="small" color="primary">
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
                find(filterData, ({ field }) => field === f.field),
                'filterObject'
              ),
              apuPartItemTypes,
            })
          )}
          {!isEmpty(relationships) && (
            <RelationshipFilter
              {...{
                relationships: relationships || [],
                onChange: onRelationshipsChange,
                apuPartItemTypes,
              }}
            />
          )}
          {filters.length || relationships !== null ? (
            <div className={spacingClasses.paddingTop}>
              <Button
                contained={true}
                label={formatMessage({ id: Message.ALL_FILTERS })}
                color="primary"
                onClick={handleAllFilters}
              />
              <FilterDialog
                ref={dialogRef}
                key={`${dialogKey}`}
                filters={filters}
                filterData={cloneDeep(filterData)}
                onConfirm={updateFiltersAndRelationships}
                onCancel={handleDialogClose}
                apuPartItemTypes={apuPartItemTypes}
                relationshipsInit={relationships}
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
