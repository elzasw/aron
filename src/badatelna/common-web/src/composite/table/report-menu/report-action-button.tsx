import React, { useRef } from 'react';
import Menu from '@material-ui/core/Menu';
import PrintIcon from '@material-ui/icons/Print';
import { TableToolbarButton } from '../table-toolbar-button';
import { useReportActionMenu } from './report-action-hook';
import { ReportActionButtonProps } from './report-action-types';
import { ReportActionItem } from './report-action-item';
import { useIntl } from 'react-intl';

export function ReportActionButton({ disabled, tag }: ReportActionButtonProps) {
  const anchorRef = useRef<HTMLSpanElement | null>(null);

  const { openMenu, closeMenu, opened, actions } = useReportActionMenu();

  const intl = useIntl();

  return (
    <>
      <TableToolbarButton
        ref={anchorRef}
        disabled={disabled || actions.length === 0}
        label={<PrintIcon />}
        onClick={openMenu}
        tooltip={intl.formatMessage({
          id: 'EAS_TABLE_REPORT_ACTION_MENU_BTN',
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
          <ReportActionItem key={i} action={action} closeMenu={closeMenu} />
        ))}
      </Menu>
    </>
  );
}
