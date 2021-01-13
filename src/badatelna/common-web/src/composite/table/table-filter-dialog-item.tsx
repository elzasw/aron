import React from 'react';
import FormControlLabel from '@material-ui/core/FormControlLabel/FormControlLabel';
import Checkbox from '@material-ui/core/Checkbox/Checkbox';
import { TableFilterDialogItemProps } from './table-types';
import { useStyles } from './table-styles';

/**
 * Filter dialog item.
 */
export function TableFilterDialogItem({
  filter,
  state,
  onToggle,
  onChangeValue,
  onChangeFilterState,
}: TableFilterDialogItemProps) {
  const classes = useStyles();

  const { FilterComponent } = filter;

  return (
    <div className={classes.filterDialogItem}>
      <FormControlLabel
        className={classes.columnDialogItemLabel}
        classes={{ label: classes.dialogCheckBoxLabel }}
        label={filter.label}
        control={
          <Checkbox
            checked={state?.enabled}
            color="primary"
            onChange={onToggle}
          />
        }
      />
      {state?.enabled && (
        <FilterComponent
          filter={filter}
          state={state}
          disabled={!state?.enabled}
          value={state.value}
          onChange={onChangeValue}
          onChangeState={onChangeFilterState}
        />
      )}
    </div>
  );
}
