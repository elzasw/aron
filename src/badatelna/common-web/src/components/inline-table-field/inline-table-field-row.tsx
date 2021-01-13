import React, { useContext } from 'react';
import { get } from 'lodash';
import RemoveIcon from '@material-ui/icons/Delete';
import { TableFieldRowProps } from 'components/table-field/table-field-types';
import { TableFieldContext } from 'components/table-field/table-field-context';
import { FormContext } from 'composite/form/form-context';
import { useEventCallback } from 'utils/event-callback-hook';
import { TableCells } from 'composite/table/table-cells';
import { useStyles } from './inline-table-field-styles';
import { InlineTableFieldContext } from './inline-table-context';

export function InlineTableFieldRow<OBJECT>({
  index,
  value,
}: TableFieldRowProps<OBJECT>) {
  const classes = useStyles();

  const { showRemoveDialog, filteredColumns } = useContext<
    TableFieldContext<OBJECT>
  >(TableFieldContext);
  const { withRemove } = useContext(InlineTableFieldContext);

  const { editing } = useContext(FormContext);

  const handleDeleteClick = useEventCallback(() => {
    showRemoveDialog(index);
  });

  return (
    <div className={classes.row}>
      <div className={classes.tableRowActions}>
        {withRemove && editing && <RemoveIcon onClick={handleDeleteClick} />}
      </div>
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
}
