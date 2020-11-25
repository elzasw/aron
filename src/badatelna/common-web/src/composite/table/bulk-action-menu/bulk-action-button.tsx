import React, { useRef } from 'react';
import Menu from '@material-ui/core/Menu';
import FilterNoneIcon from '@material-ui/icons/Layers';
import { TableToolbarButton } from '../table-toolbar-button';
import { useBulkActionMenu } from './bulk-action-hook';
import { BulkActionButtonProps } from './bulk-action-types';
import { BulkActionItem } from './bulk-action-item';
import { useIntl } from 'react-intl';

export function BulkActionButton({ disabled, actions }: BulkActionButtonProps) {
  const anchorRef = useRef<HTMLSpanElement | null>(null);

  const { openMenu, closeMenu, opened } = useBulkActionMenu();

  const intl = useIntl();

  return (
    <>
      <TableToolbarButton
        ref={anchorRef}
        disabled={disabled}
        label={<FilterNoneIcon />}
        onClick={openMenu}
        tooltip={intl.formatMessage({
          id: 'EAS_TABLE_BULK_ACTION_MENU_BTN',
          defaultMessage: 'Hromadné akce',
        })}
      />
      <Menu
        anchorEl={anchorRef.current}
        keepMounted
        MenuListProps={{
          disablePadding: true,
        }}
        open={opened}
        onClose={closeMenu}
      >
        {actions.map((action, i) => (
          <BulkActionItem key={i} action={action} closeMenu={closeMenu} />
        ))}
      </Menu>
    </>
  );
}
