import React, { useRef } from 'react';
import Menu from '@material-ui/core/Menu';
import PrintIcon from '@material-ui/icons/Print';
import { TableToolbarButton } from '../table-toolbar-button';
import { useExportActionMenu } from './export-action-hook';
import { ExportActionButtonProps } from './export-action-types';
import { ExportActionItem } from './export-action-item';
import { useIntl } from 'react-intl';

export function ExportActionButton({ disabled, tag }: ExportActionButtonProps) {
  const anchorRef = useRef<HTMLSpanElement | null>(null);

  const { openMenu, closeMenu, opened, actions } = useExportActionMenu();

  const intl = useIntl();

  return (
    <>
      <TableToolbarButton
        ref={anchorRef}
        disabled={disabled || actions.length === 0}
        label={<PrintIcon />}
        onClick={openMenu}
        tooltip={intl.formatMessage({
          id: 'EAS_TABLE_EXPORT_ACTION_MENU_BTN',
          defaultMessage: 'Tisk',
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
          <ExportActionItem key={i} action={action} closeMenu={closeMenu} />
        ))}
      </Menu>
    </>
  );
}
