import React, { useContext, forwardRef } from 'react';
import clsx from 'clsx';
import Typography from '@material-ui/core/Typography';
import Draggable from 'react-draggable';
import { useStyles } from './table-field-styles';
import { TableFieldContext } from './table-field-context';

export const TableFieldHeader = forwardRef<HTMLDivElement, any>(
  function TableFieldHeader(_, ref) {
    const classes = useStyles();

    const { filteredColumns, setColumnWidth } = useContext(TableFieldContext);

    return (
      <div ref={ref} className={classes.header}>
        <div className={classes.tableRowActions}></div>
        {filteredColumns.map((column, i) => {
          const { name, datakey } = column;

          return (
            <div
              key={`${datakey}-${i}`}
              className={classes.tableRowHeader}
              style={{ width: column.width }}
            >
              <Typography className={classes.tableRowHeaderLabel} variant="h6">
                {name}
              </Typography>
              <div
                className={clsx(classes.draggable, classes.headerCellDraggable)}
                onClick={(event) => event.stopPropagation()}
              >
                <Draggable
                  axis="x"
                  defaultClassNameDragging={classes.draggable}
                  onStop={(_, { x }) =>
                    setColumnWidth(datakey, column.width + x)
                  }
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
  }
);
