import React, { useContext } from 'react';
import ControlPointIcon from '@material-ui/icons/ControlPoint';
import { useStyles } from './inline-table-field-styles';
import { TableFieldContext } from 'components/table-field/table-field-context';
import { useEventCallback } from 'utils/event-callback-hook';
import { TableFieldToolbarButton } from 'components/table-field/table-field-toolbar-button';
import { FormattedMessage } from 'react-intl';
import { InlineTableFieldContext } from './inline-table-context';

export function InlineTableFieldToolbar() {
  const { disabled, disabledAdd, visibleAdd, saveRow, value } = useContext(
    TableFieldContext
  );
  const { initNewItem } = useContext(InlineTableFieldContext);

  const classes = useStyles();

  const handleAddClick = useEventCallback(() =>
    saveRow(value.length + 1 ?? 0, initNewItem())
  );

  return (
    <div className={classes.tableActions}>
      <div className={classes.buttonGroup}>
        <TableFieldToolbarButton
          title={
            <FormattedMessage
              id="EAS_INLINE_TABLE_FIELD_TOOLBAR_BTN_ADD"
              defaultMessage="Přidat"
            />
          }
          onClick={handleAddClick}
          IconComponent={ControlPointIcon}
          disabled={disabled || disabledAdd}
          show={visibleAdd}
        />
      </div>
    </div>
  );
}
