import React, { forwardRef, useState, useEffect, useCallback } from 'react';
import classNames from 'classnames';
import { get, find } from 'lodash';
import { FormattedMessage, useIntl } from 'react-intl';

import { DialogHandle, Dialog } from '@eas/common-web';

import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../../styles';
import { getFilterComponent } from '../utils';
import { FilterData, FilterObject } from '../types';
import { ApuPartItemType, Relationship } from '../../../types';
import { Message } from '../../../enums';
import RelationshipFilter from './relationship-filter';

interface Props {
  filters: any[];
  filterData: FilterData[];
  onConfirm: (
    filterData: FilterData[],
    newRelationships: Relationship[]
  ) => void;
  onCancel: () => void;
  apuPartItemTypes: ApuPartItemType[];
  relationshipsInit: Relationship[] | null;
}

export const FilterDialog = forwardRef<DialogHandle, Props>(
  function FilterDialog(
    {
      filters,
      filterData,
      onConfirm,
      onCancel,
      apuPartItemTypes,
      relationshipsInit,
    },
    ref
  ) {
    const classes = useStyles();
    const layoutClasses = useLayoutStyles();
    const spacingClasses = useSpacingStyles();

    const { formatMessage } = useIntl();

    const [relationships, setRelationships] = useState<Relationship[] | null>(
      relationshipsInit
    );

    const relationshipItem = {
      label: <FormattedMessage id={Message.RELATIONSHIPS} />,
      relationshipsSelected: true,
    };

    const [current, setCurrent] = useState<any>(filters[0] || relationshipItem);

    const [newFilterData, setNewFilterData] = useState<FilterData[]>(
      filterData
    );

    const onFilterChange = useCallback(
      (field: string, filterObject: FilterObject | null) => {
        setNewFilterData([
          ...newFilterData.filter((f) => f.field !== field),
          ...(filterObject ? [{ field, filterObject }] : []),
        ]);
      },
      [newFilterData]
    );

    const handleConfirm = () => {
      onConfirm(newFilterData, relationships || []);
    };

    const handleCancel = () => onCancel();

    useEffect(() => {
      setNewFilterData(filterData);
    }, [filterData]);

    const filterComponent = current.relationshipsSelected ? (
      <RelationshipFilter
        {...{
          inDialog: true,
          relationships: relationships || [],
          onChange: setRelationships,
          apuPartItemTypes,
        }}
      />
    ) : (
      getFilterComponent({
        ...current,
        onChange: onFilterChange,
        value: get(
          find(newFilterData, ({ field }) => field === current.field),
          'filterObject'
        ),
        inDialog: true,
        apuPartItemTypes,
      })
    );

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
              {[...filters, relationshipsInit !== null && relationshipItem].map(
                (item) => (
                  <div
                    key={item.label}
                    className={classNames(
                      classes.filterDialogItem,
                      item.label === current.label &&
                        classes.filterDialogItemActive
                    )}
                    onClick={() => setCurrent(item)}
                  >
                    <div className={spacingClasses.padding}>{item.label}</div>
                  </div>
                )
              )}
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
