import React, { forwardRef, useState, useEffect, useCallback } from 'react';
import classNames from 'classnames';
import { get, find } from 'lodash';

import { DialogHandle, Dialog } from '@eas/common-web';

import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../../styles';
import { getFilterComponent } from '../utils';
import { FilterData, FilterObject } from '../types';

interface Props {
  filters: any[];
  filterData: FilterData[];
  onConfirm: (filterData: FilterData[]) => void;
  onCancel: () => void;
}

export const FilterDialog = forwardRef<DialogHandle, Props>(
  function FilterDialog({ filters, filterData, onConfirm, onCancel }, ref) {
    const classes = useStyles();
    const layoutClasses = useLayoutStyles();
    const spacingClasses = useSpacingStyles();

    const [current, setCurrent] = useState<any>(filters[0]);

    const [newFilterData, setNewFilterData] = useState<FilterData[]>(
      filterData
    );

    const onFilterChange = useCallback(
      (name: string, filterObject: FilterObject | null) => {
        setNewFilterData([
          ...newFilterData.filter((f) => f.name !== name),
          ...(filterObject ? [{ name, filterObject }] : []),
        ]);
      },
      [newFilterData]
    );

    const handleConfirm = () => {
      onConfirm(newFilterData);
    };

    const handleCancel = () => onCancel();

    useEffect(() => {
      setNewFilterData(filterData);
    }, [filterData]);

    const filterComponent = getFilterComponent({
      ...current,
      onChange: onFilterChange,
      value: get(
        find(newFilterData, ({ name }) => name === current.name),
        'filterObject'
      ),
      inDialog: true,
    });

    return (
      <Dialog
        ref={ref}
        title="Všechny filtry"
        onConfirm={handleConfirm}
        onCancel={handleCancel}
      >
        {() => (
          <div className={classNames(classes.filterDialog, layoutClasses.flex)}>
            <div className={classes.filterDialogLeft}>
              {filters.map((item) => (
                <div
                  key={item.title}
                  className={classNames(
                    classes.filterDialogItem,
                    item.title === current.title &&
                      classes.filterDialogItemActive
                  )}
                  onClick={() => setCurrent(item)}
                >
                  <div className={spacingClasses.padding}>{item.title}</div>
                </div>
              ))}
            </div>
            <div className={classes.filterDialogRight}>
              <div className={spacingClasses.padding}>{filterComponent}</div>
            </div>
          </div>
        )}
      </Dialog>
    );
  }
);
