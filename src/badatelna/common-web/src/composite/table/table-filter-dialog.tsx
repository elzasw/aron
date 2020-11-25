import React, { forwardRef, useContext, useState } from 'react';
import { findIndex } from 'lodash';
import { FormattedMessage } from 'react-intl';
import { useEventCallback } from 'utils/event-callback-hook';
import { Dialog } from 'components/dialog/dialog';
import { DialogHandle } from 'components/dialog/dialog-types';
import { TableContext } from './table-context';
import { TableFilter, TableFilterState } from './table-types';
import { TableFilterDialogItem } from './table-filter-dialog-item';
import { useStyles } from './table-styles';

export const TableFilterDialog = forwardRef<DialogHandle, any>(
  function TableFilterDialog(props, ref) {
    const classes = useStyles();

    const {
      filters,
      filtersState: providedFiltersState,
      setFiltersState: setProvidedFiltersState,
    } = useContext(TableContext);

    /** Internal copy of filtersState */
    const [filtersState, setFiltersState] = useState<TableFilterState[]>([]);

    /**
     * Resets internal filters state.
     */
    const handleShow = useEventCallback(() => {
      setFiltersState(providedFiltersState);
    });

    /**
     * Saves current filters states.
     */
    const handleSave = useEventCallback(() => {
      setProvidedFiltersState(filtersState);
    });

    /**
     * Toggles the enable flag on selected filter.
     */
    const handleFilterToggle = useEventCallback((index: number) => {
      setFiltersState((filtersState) => [
        ...filtersState.slice(0, index),
        { ...filtersState[index], enabled: !filtersState[index].enabled },
        ...filtersState.slice(index + 1),
      ]);
    });

    /**
     * Changes the value of the filter.
     */
    const handleFilterValueChange = useEventCallback(
      (index: number, value: any) => {
        setFiltersState((filtersState) => [
          ...filtersState.slice(0, index),
          { ...filtersState[index], value },
          ...filtersState.slice(index + 1),
        ]);
      }
    );

    const handleFilterStateChange = useEventCallback(
      (index: number, state: TableFilterState) => {
        setFiltersState((filtersState) => [
          ...filtersState.slice(0, index),
          state,
          ...filtersState.slice(index + 1),
        ]);
      }
    );

    /**
     * Split UI of filter dialog into columns depending on count of filters
     */
    let COLUMNS = 0;
    const filterCols: TableFilter[][] = [];

    if (filters.length < 3) {
      COLUMNS = 1;
    } else if (filters.length < 5) {
      COLUMNS = 2;
    } else {
      COLUMNS = 3;
    }

    filters.forEach((filter, i) => {
      if (!filterCols[i % COLUMNS]) {
        filterCols.push([]);
      }
      filterCols[i % COLUMNS].push(filter);
    });

    return (
      <Dialog
        ref={ref}
        title={
          <FormattedMessage
            id="EAS_TABLE_FILTER_DIALOG_TITLE"
            defaultMessage="Nastavení filtrů"
          />
        }
        confirmLabel={
          <FormattedMessage
            id="EAS_TABLE_FILTER_DIALOG_BTN_SAVE"
            defaultMessage="Uložit"
          />
        }
        onConfirm={handleSave}
        onShow={handleShow}
      >
        {() => (
          <div className={classes.filterDialogColumnsWrapper}>
            {filterCols.map((col, i) => {
              return (
                <div key={i}>
                  {col.map((filter) => {
                    const index = findIndex(
                      filters,
                      (f) => f.filterkey === filter.filterkey
                    );
                    const state = filtersState[index];

                    return (
                      <TableFilterDialogItem
                        key={index}
                        filter={filter}
                        state={state}
                        onToggle={() => handleFilterToggle(index)}
                        onChangeValue={(value) =>
                          handleFilterValueChange(index, value)
                        }
                        onChangeFilterState={(state) =>
                          handleFilterStateChange(index, state)
                        }
                      />
                    );
                  })}
                </div>
              );
            })}
          </div>
        )}
      </Dialog>
    );
  }
);
