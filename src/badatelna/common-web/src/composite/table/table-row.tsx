import React, { useContext, memo, ReactElement } from 'react';
import clsx from 'clsx';
import { get } from 'lodash';
import { DomainObject } from 'common/common-types';
import {
  TableRowProps,
  TableColumnAlign,
  TableColumnValueMapper,
} from './table-types';
import { useStyles } from './table-styles';
import { TableContext, TableSelectedContext } from './table-context';
import { TableRowSelection } from './table-row-selection';
import { useEventCallback } from 'utils/event-callback-hook';

export const TableRow = memo(function TableFieldRow<
  OBJECT extends DomainObject
>({ index, value }: TableRowProps<OBJECT>) {
  const classes = useStyles();

  const { filteredColumns, setActiveRow, showSelectBox, sorts } = useContext<
    TableContext<OBJECT>
  >(TableContext);

  const { activeRow } = useContext(TableSelectedContext);

  const handleClick = useEventCallback(() => {
    setActiveRow(value.id);
  });

  const defaultValueMapper: TableColumnValueMapper<
    OBJECT,
    any
  > = useEventCallback(({ value }) => value);

  return (
    <div
      className={clsx(classes.row, {
        [classes.rowActive]: activeRow === value.id,
      })}
      onClick={handleClick}
    >
      {showSelectBox && (
        <div className={classes.tableRowActions}>
          <TableRowSelection value={value} />
        </div>
      )}
      {filteredColumns.map((column, i) => {
        const { CellComponent, valueMapper = defaultValueMapper } = column;
        return (
          <div
            key={i}
            className={clsx(classes.cellWrapper, {
              [classes.columnAlignLeft]: column.align === TableColumnAlign.LEFT,
              [classes.columnAlignRight]:
                column.align === TableColumnAlign.RIGHT,
              [classes.columnAlignCenter]:
                column.align === TableColumnAlign.CENTER,
            })}
            style={{
              width: column.width,
            }}
          >
            <CellComponent
              index={index}
              value={valueMapper({
                rowValue: value,
                column,
                value: get(value, column.displaykey ?? column.datakey, ''),
                sorts,
              })}
              rowValue={value}
              column={column}
            />
          </div>
        );
      })}
    </div>
  );
}) as <OBJECT>(p: TableRowProps<OBJECT>) => ReactElement;
