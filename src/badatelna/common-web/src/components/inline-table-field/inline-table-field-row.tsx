import React, { useContext, forwardRef, Ref, memo, ReactElement } from 'react';
import { get } from 'lodash';
import RemoveIcon from '@material-ui/icons/Delete';
import { TableFieldRowProps } from 'components/table-field/table-field-types';
import { TableFieldContext } from 'components/table-field/table-field-context';
import { FormContext } from 'composite/form/form-context';
import { useEventCallback } from 'utils/event-callback-hook';
import { TableCells } from 'composite/table/table-cells';
import { useStyles } from './inline-table-field-styles';
import { InlineTableFieldContext } from './inline-table-context';
import { DragHandle } from 'components/table-field/table-field-drag-handle';

export const InlineTableFieldRow = memo(
  forwardRef(function InlineTableFieldRow<OBJECT>(
    { index, value }: TableFieldRowProps<OBJECT>,
    ref: Ref<HTMLDivElement>
  ) {
    const classes = useStyles();

    const {
      showRemoveDialog,
      filteredColumns,
      disabled,
      useDnDOrdering,
      visibleActionsColumn,
    } = useContext<TableFieldContext<OBJECT>>(TableFieldContext);
    const { withRemove } = useContext(InlineTableFieldContext);

    const { editing } = useContext(FormContext);

    const handleDeleteClick = useEventCallback(() => {
      showRemoveDialog(index);
    });

    return (
      <div className={classes.row} ref={ref}>
        {visibleActionsColumn && (
          <div className={classes.tableRowActions}>
            {!disabled && useDnDOrdering && (
              <DragHandle className={classes.rowDraggable} />
            )}
            {withRemove && editing && !disabled && (
              <RemoveIcon onClick={handleDeleteClick} />
            )}
          </div>
        )}
        {filteredColumns.map((column, i) => {
          const { CellComponent = TableCells.TextCell } = column;
          return (
            <div
              key={i}
              className={classes.cellWrapper}
              style={{
                width: column.width,
              }}
            >
              <CellComponent
                index={index}
                value={get(value, column.datakey, '')}
                rowValue={value}
                column={column as any}
              />
            </div>
          );
        })}
      </div>
    );
  }) as <OBJECT>(p: TableFieldRowProps<OBJECT>) => ReactElement
);
