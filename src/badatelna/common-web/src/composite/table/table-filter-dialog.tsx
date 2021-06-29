import React, { forwardRef, useContext, useState } from 'react';
import _, { findIndex, groupBy } from 'lodash';
import { FormattedMessage } from 'react-intl';
import Button from '@material-ui/core/Button';
import Typography from '@material-ui/core/Typography';
import { useEventCallback } from 'utils/event-callback-hook';
import { Dialog } from 'components/dialog/dialog';
import { DialogHandle } from 'components/dialog/dialog-types';
import { TableContext } from './table-context';
import { TableFilterState } from './table-types';
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
      const validFilterState = filtersState.map((filter) => {
        if (
          filter.enabled &&
          filter.value === null &&
          !filter.filters?.length
        ) {
          return {
            ...filter,
            enabled: false,
          };
        }

        return filter;
      });

      setProvidedFiltersState(validFilterState);
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

    const handleRemoveAllFilters = useEventCallback(() => {
      setFiltersState(
        filtersState.map((state) => ({
          ...state,
          value: null,
          enabled: false,
          object: null,
          filters: [],
          numberFilter: null,
        }))
      );
    });

    /**
     * Split UI of filter dialog into columns depending on count of filters
     */
    let COLUMNS = 0;

    if (filters.length < 3) {
      COLUMNS = 1;
    } else if (filters.length < 5) {
      COLUMNS = 2;
    } else {
      COLUMNS = 3;
    }

    const groupedFilters = groupBy(filters, 'filterGroup');

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
        actions={[
          <Button
            key="removeAll"
            variant="outlined"
            onClick={handleRemoveAllFilters}
          >
            <Typography classes={{ root: classes.buttonLabel }}>
              <FormattedMessage
                id="EAS_TABLE_FILTER_DIALOG_BTN_REMOVE_ALL_FILTERS"
                defaultMessage="Smazat filtry"
              />
            </Typography>
          </Button>,
        ]}
      >
        {() => (
          <div className={classes.filterDialogColumnsWrapper}>
            {Object.keys(groupedFilters).map((key) => {
              return (
                <>
                  <div
                    style={{
                      maxWidth: 900,
                      display: 'flex',
                      flexDirection: 'column',
                    }}
                  >
                    {_(groupedFilters[key])
                      .orderBy('filterOrder')
                      .chunk(COLUMNS)
                      .value()
                      .map((filterChunk, i) => (
                        <div key={i} style={{ display: 'flex' }}>
                          {filterChunk.map((filter) => {
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
                      ))}
                  </div>
                  <hr
                    style={{
                      border: '1px solid rgba(0, 0, 0, 0.12)',
                      height: 1,
                      margin: '10px 0',
                    }}
                  />
                </>
              );
            })}
          </div>
        )}
      </Dialog>
    );
  }
);
