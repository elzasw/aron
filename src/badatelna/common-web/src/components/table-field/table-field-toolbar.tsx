import React, { useContext } from 'react';
import { FormattedMessage } from 'react-intl';
import ControlPointIcon from '@material-ui/icons/ControlPoint';
import RemoveIcon from '@material-ui/icons/Delete';
import EditIcon from '@material-ui/icons/Edit';
import { useEventCallback } from 'utils/event-callback-hook';
import { useStyles } from './table-field-styles';
import { TableFieldContext } from './table-field-context';
import { TableFieldToolbarButton } from './table-field-toolbar-button';
import { TableFieldToolbarProps } from './table-field-types';

export function TableFieldToolbar({ selectedIndex }: TableFieldToolbarProps) {
  const classes = useStyles();

  const {
    showAddDialog,
    showEditDialog,
    showRemoveDialog,
    disabled,
    disabledAdd,
    disabledEdit,
    disabledRemove,
    visibleAdd,
    visibleEdit,
    visibleRemove,
  } = useContext(TableFieldContext);

  const handleAddClick = useEventCallback(() => showAddDialog());

  const handleEditClick = useEventCallback(() => {
    if (selectedIndex !== undefined) {
      showEditDialog(selectedIndex);
    }
  });

  const handleRemoveClick = useEventCallback(() => {
    if (selectedIndex !== undefined) {
      showRemoveDialog(selectedIndex);
    }
  });

  return (
    <div className={classes.tableActions}>
      <div className={classes.buttonGroup}>
        <TableFieldToolbarButton
          title={
            <FormattedMessage
              id="EAS_TABLE_FIELD_TOOLBAR_BTN_ADD"
              defaultMessage="Přidat"
            />
          }
          onClick={handleAddClick}
          IconComponent={ControlPointIcon}
          disabled={disabled || disabledAdd}
          show={visibleAdd}
        ></TableFieldToolbarButton>
        <TableFieldToolbarButton
          title={
            <FormattedMessage
              id="EAS_TABLE_FIELD_TOOLBAR_BTN_EDIT"
              defaultMessage="Upravit"
            />
          }
          onClick={handleEditClick}
          IconComponent={EditIcon}
          disabled={disabled || disabledEdit || selectedIndex === undefined}
          show={visibleEdit}
        ></TableFieldToolbarButton>
        <TableFieldToolbarButton
          title={
            <FormattedMessage
              id="EAS_TABLE_FIELD_TOOLBAR_BTN_DELETE"
              defaultMessage="Smazat"
            />
          }
          onClick={handleRemoveClick}
          IconComponent={RemoveIcon}
          disabled={disabled || disabledRemove || selectedIndex === undefined}
          show={visibleRemove}
        ></TableFieldToolbarButton>
      </div>
    </div>
  );
}
