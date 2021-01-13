import React, { useState, useContext, forwardRef } from 'react';
import { FormattedMessage } from 'react-intl';
import Button from '@material-ui/core/Button';
import Typography from '@material-ui/core/Typography';
import { useEventCallback } from 'utils/event-callback-hook';
import { arrayMove } from 'utils/array-move';
import {
  SortableContainer,
  SortableElement,
  SortableHandle,
} from 'react-sortable-hoc';
import FormControlLabel from '@material-ui/core/FormControlLabel/FormControlLabel';
import Checkbox from '@material-ui/core/Checkbox/Checkbox';
import DragIndicatorIcon from '@material-ui/icons/DragIndicator';
import { Dialog } from 'components/dialog/dialog';
import { DialogHandle } from 'components/dialog/dialog-types';
import { TableColumnState, TableColumn } from './table-types';
import { TableContext } from './table-context';
import { useStyles } from './table-styles';

/**
 * Column dialog.
 */
export const TableColumnDialog = forwardRef<DialogHandle, any>(
  function TableColumnDialog(props, ref) {
    const classes = useStyles();

    const {
      setColumnsState: setProvidedFiltersState,
      defaultColumnsState,
      columns,
      columnsState: providedColumnsState,
    } = useContext(TableContext);

    const [columnsState, setColumnsState] = useState<TableColumnState[]>([]);

    /**
     * Toggles the visibility flag on selected column.
     */
    const toggleColumnVisibility = useEventCallback((datakey: string) => {
      setColumnsState((columnsState) => {
        const index = columnsState.findIndex(
          (column) => column.datakey === datakey
        );

        if (index !== -1) {
          return [
            ...columnsState.slice(0, index),
            { ...columnsState[index], visible: !columnsState[index].visible },
            ...columnsState.slice(index + 1),
          ];
        } else {
          return columnsState;
        }
      });
    });

    /**
     * Swaps column positions.
     */
    const swapColumns = useEventCallback(
      ({ oldIndex, newIndex }: { oldIndex: number; newIndex: number }) => {
        setColumnsState((columnsState) =>
          arrayMove(columnsState, oldIndex, newIndex)
        );
      }
    );

    /**
     * Saves current column states.
     */
    const handleSave = useEventCallback(() => {
      setProvidedFiltersState(columnsState);
    });

    /**
     * Resets internal column state.
     */
    const handleShow = useEventCallback(() => {
      setColumnsState(providedColumnsState);
    });

    const handleReset = useEventCallback(() => {
      setColumnsState(defaultColumnsState);
    });

    return (
      <Dialog
        ref={ref}
        title={
          <FormattedMessage
            id="EAS_TABLE_COLUMN_DIALOG_TITLE"
            defaultMessage="Nastavení sloupců"
          />
        }
        confirmLabel={
          <FormattedMessage
            id="EAS_TABLE_COLUMN_DIALOG_BTN_SAVE"
            defaultMessage="Uložit"
          />
        }
        onConfirm={handleSave}
        onShow={handleShow}
        actions={[
          <Button key="removeAll" variant="outlined" onClick={handleReset}>
            <Typography classes={{ root: classes.buttonLabel }}>
              <FormattedMessage
                id="EAS_TABLE_COLUMN_DIALOG_BTN_RESET"
                defaultMessage="Obnovit"
              />
            </Typography>
          </Button>,
        ]}
      >
        {() => (
          <div>
            <SortableColumnList
              useDragHandle
              lockAxis="y"
              onSortEnd={swapColumns}
              columnsState={columnsState}
              columns={columns}
              onVisibleToggle={toggleColumnVisibility}
            />
          </div>
        )}
      </Dialog>
    );
  }
);

/**
 * Whole list of columns entries with sortable functionality.
 */
const SortableColumnList = SortableContainer(function SortableColumnList({
  columnsState,
  columns,
  onVisibleToggle,
}: {
  columnsState: TableColumnState[];
  columns: TableColumn<any, any>[];
  onVisibleToggle: (datakey: string) => void;
}) {
  const classes = useStyles();

  return (
    <ul className={classes.columnDialogList}>
      {columnsState.map((state, i) => {
        const column = columns.find(
          (column) => column.datakey === state.datakey
        )!;

        return (
          <SortableColumnEntry
            key={state.datakey}
            index={i}
            column={column}
            columnState={state}
            onVisibleToggle={onVisibleToggle}
          />
        );
      })}
    </ul>
  );
});

/**
 * Draggable handle to change column order.
 */
const DragHandle = SortableHandle(({ className }: { className: string }) => (
  <DragIndicatorIcon className={className} />
));

/**
 * One column entry with sortable functionality.
 */
const SortableColumnEntry = SortableElement(function ColumnEntry({
  columnState,
  column,
  onVisibleToggle,
}: {
  columnState: TableColumnState;
  column: TableColumn<any, any>;
  onVisibleToggle: (datakey: string) => void;
}) {
  const classes = useStyles();

  return (
    <li className={classes.columnDialogItem}>
      <FormControlLabel
        className={classes.columnDialogItemLabel}
        classes={{ label: classes.dialogCheckBoxLabel }}
        label={column.name}
        control={
          <Checkbox
            disabled={column.fixed}
            checked={columnState.visible}
            color="primary"
            onChange={() => onVisibleToggle(columnState.datakey)}
          />
        }
      />
      <DragHandle className={classes.columnDialogItemDraggable} />
    </li>
  );
});
