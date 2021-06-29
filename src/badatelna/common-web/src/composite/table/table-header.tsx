import React, { useContext, forwardRef } from 'react';
import clsx from 'clsx';
import Draggable from 'react-draggable';
import { FormattedMessage } from 'react-intl';
import { useEventCallback } from 'utils/event-callback-hook';
import Typography from '@material-ui/core/Typography';
import ArrowDropDownIcon from '@material-ui/icons/ArrowDropDown';
import { Checkbox } from 'components/checkbox/checkbox';
import { useStyles } from './table-styles';
import { TableContext, TableSelectedContext } from './table-context';
import { TableColumn } from './table-types';
import { useScrollBarSize } from 'utils/use-scrollbar-size';
import { Tooltip } from 'components/tooltip/tooltip';

export const TableHeader = forwardRef<HTMLDivElement, any>(function TableHeader(
  _,
  ref
) {
  const classes = useStyles();

  const {
    filteredColumns,
    sorts,
    setColumnsState,
    columns,
    columnsState,
    toggleSortColumn,
    toggleAllRowSelection,
    showSelectBox,
  } = useContext(TableContext);

  const { selected } = useContext(TableSelectedContext);

  const setColumnWidth = useEventCallback((datakey: string, width: number) => {
    const index = columnsState.findIndex(
      (column) => column.datakey === datakey
    );

    if (index !== -1) {
      setColumnsState([
        ...columnsState.slice(0, index),
        {
          ...columnsState[index],
          width: Math.max(columns[index].minWidth ?? 50, width),
        },
        ...columnsState.slice(index + 1, undefined),
      ]);
    }
  });

  const handleHeaderClick = useEventCallback((column: TableColumn<any>) => {
    if (column.sortable) {
      toggleSortColumn(column.datakey);
    }
  });

  const size = useScrollBarSize();

  return (
    <div ref={ref} className={classes.header}>
      <div style={{ width: size }}></div>
      {showSelectBox && (
        <Tooltip
          title={
            <FormattedMessage
              id="EAS_TABLE_SELECT_ALL"
              defaultMessage="Výběr položek slouží jako vstup pro hromadné operace (např. tisk)"
            />
          }
          placement="top-start"
        >
          <div className={classes.tableRowActions}>
            <Checkbox
              highlighted={false}
              value={selected.length > 0}
              onChange={toggleAllRowSelection}
            />
          </div>
        </Tooltip>
      )}
      {filteredColumns.map((column, i) => {
        const { name, datakey } = column;

        const sortIndex = sorts.findIndex((sort) => sort.datakey === datakey);
        const sort = sortIndex !== -1 ? sorts[sortIndex] : undefined;

        return (
          <div
            key={`${datakey}-${i}`}
            className={clsx(classes.tableRowHeader, {
              [classes.sortable]: column.sortable,
            })}
            style={{ width: column.width }}
            onClick={() => handleHeaderClick(column)}
          >
            <Typography className={classes.tableRowHeaderLabel} variant="h6">
              {name}
            </Typography>
            {sort && (
              <>
                <ArrowDropDownIcon
                  classes={{
                    root: sort.order === 'ASC' ? classes.iconRotate : '',
                  }}
                />
                <Typography variant="h6" className={classes.sortSup}>
                  {sortIndex + 1}
                </Typography>
              </>
            )}

            <div
              className={clsx(classes.draggable, classes.headerCellDraggable)}
              onClick={(event) => event.stopPropagation()}
            >
              <Draggable
                axis="x"
                defaultClassNameDragging={classes.draggable}
                onStop={(_, { x }) => setColumnWidth(datakey, column.width + x)}
                position={{ x: 0, y: 0 }}
              >
                <div className={classes.draggableIcon}>⋮</div>
              </Draggable>
            </div>
          </div>
        );
      })}
    </div>
  );
});
