import React, { ComponentType } from 'react';
import { Filter, ApiFilterOperation } from 'common/common-types';
import Typography from '@material-ui/core/Typography';
import { FilterComponentProps, TableFilterOperation } from '../table-types';
import { useEventCallback } from 'utils/event-callback-hook';
import { FormattedMessage } from 'react-intl';
import { useStyles } from '../table-styles';

export function filterIntervalCellFactory<VALUE_TYPE>(
  Component: ComponentType<{
    disabled: boolean;
    value: VALUE_TYPE | null;
    onChange: (value: VALUE_TYPE | null) => void;
  }>
) {
  interface InternalFilter {
    from: VALUE_TYPE | null;
    to: VALUE_TYPE | null;
  }

  return function FilterIntervalCell({
    filter,
    disabled,
    state,
    onChangeState,
  }: FilterComponentProps) {
    const classes = useStyles();

    const { from, to }: InternalFilter = state.numberFilter ?? {
      from: null,
      to: null,
    };

    const handleChangeFrom = useEventCallback((value: VALUE_TYPE | null) => {
      onChangeState({
        ...state,
        operation: TableFilterOperation.AND,
        filters: constructNestedFilters(filter.filterkey, value, to),
        numberFilter: { from: value, to },
      });
    });

    const handleChangeTo = useEventCallback((value: VALUE_TYPE | null) => {
      onChangeState({
        ...state,
        operation: TableFilterOperation.AND,
        filters: constructNestedFilters(filter.filterkey, from, value),
        numberFilter: { from, to: value },
      });
    });

    return (
      <>
        <div className={classes.filterDialogItemSubWrapper}>
          <Typography>
            <FormattedMessage
              id="EAS_TABLE_FILTER_CELL_INTERVAL_FROM"
              defaultMessage="Od"
            />
          </Typography>
          <div className={classes.filterDialogItemValueWrapper}>
            <Component
              disabled={disabled}
              value={from}
              onChange={handleChangeFrom}
            />
          </div>
        </div>
        <div className={classes.filterDialogItemSubWrapper}>
          <Typography>
            <FormattedMessage
              id="EAS_TABLE_FILTER_CELL_INTERVAL_TO"
              defaultMessage="Do"
            />
          </Typography>
          <div className={classes.filterDialogItemValueWrapper}>
            <Component
              disabled={disabled}
              value={to}
              onChange={handleChangeTo}
            />
          </div>
        </div>
      </>
    );
  };
}

function constructNestedFilters<VALUE_TYPE>(
  field: string,
  from: VALUE_TYPE | null,
  to: VALUE_TYPE | null
) {
  const fromFilter: Filter | undefined =
    from !== null
      ? {
          field,
          operation: ApiFilterOperation.GTE,
          value: from,
        }
      : undefined;

  const toFilter: Filter | undefined =
    to !== null
      ? {
          field: field,
          operation: ApiFilterOperation.LTE,
          value: to,
        }
      : undefined;

  return [fromFilter, toFilter].filter((f) => f !== undefined) as Filter[];
}
