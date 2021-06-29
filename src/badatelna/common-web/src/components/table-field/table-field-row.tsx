import React, { useContext, memo, ReactElement, forwardRef, Ref } from 'react';
import { get } from 'lodash';
import { useEventCallback } from 'utils/event-callback-hook';
import Radio from '@material-ui/core/Radio/Radio';
import OpenInNewIcon from '@material-ui/icons/OpenInNew';
import { Tooltip } from 'components/tooltip/tooltip';
import { TableFieldRowProps } from './table-field-types';
import { useStyles } from './table-field-styles';
import { TableFieldContext } from './table-field-context';
import { TextCell } from './cells/text-cell';
import { DragHandle } from './table-field-drag-handle';
import clsx from 'clsx';

export const TableFieldRow = memo(
  forwardRef(function TableFieldRow<OBJECT>(
    { index, value, selected }: TableFieldRowProps<OBJECT>,
    ref: Ref<HTMLDivElement>
  ) {
    const classes = useStyles();

    const {
      disabled,
      selectRow,
      showDetailDialog,
      filteredColumns,
      showDetailBtnCond,
      showRadioCond,
      useDnDOrdering,
      onSelect,
      visibleActionsColumn,
    } = useContext<TableFieldContext<OBJECT>>(TableFieldContext);

    const handleSelectClick = useEventCallback(() => {
      if (showRadioCond(value) && !disabled) {
        onSelect?.(value, index);
        selectRow(index);
      }
    });

    const handleDetailClick = useEventCallback(() => {
      showDetailDialog(index);
    });

    return (
      <div
        className={clsx(classes.row, {
          [classes.defaultCursor]:
            !showRadioCond(value) && !showDetailBtnCond(value),
        })}
        onClick={handleSelectClick}
        ref={ref}
      >
        {visibleActionsColumn && (
          <div className={classes.tableRowActions}>
            {!disabled && useDnDOrdering && (
              <DragHandle className={classes.rowDraggable} />
            )}
            {!disabled && showRadioCond(value) && (
              <Radio
                className={classes.radioButton}
                checked={selected}
                onChange={handleSelectClick}
                color="primary"
              />
            )}
            {disabled && showDetailBtnCond(value) && (
              <Tooltip title="Otevře krátký detail" placement="top-start">
                <OpenInNewIcon onClick={handleDetailClick} />
              </Tooltip>
            )}
          </div>
        )}
        {filteredColumns.map((column, i) => {
          const { CellComponent = TextCell } = column;
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
                column={column}
              />
            </div>
          );
        })}
      </div>
    );
  }) as <OBJECT>(p: TableFieldRowProps<OBJECT>) => ReactElement
);
