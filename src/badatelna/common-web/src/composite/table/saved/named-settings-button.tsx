import React, { useRef } from 'react';
import Menu from '@material-ui/core/Menu';
import SaveIcon from '@material-ui/icons/Save';
import MenuItem from '@material-ui/core/MenuItem';
import Divider from '@material-ui/core/Divider';
import { TableToolbarButton } from '../table-toolbar-button';
import { useIntl, FormattedMessage } from 'react-intl';
import { NamedSettingsButtonProps } from './named-settings-types';
import { useNamedSettingsMenu } from './named-settings-hook';
import { NamedSettingsItem } from './named-settings-item';
import { ConfirmDialog } from 'composite/confirm-dialog/confirm-dialog';
import { noop } from 'lodash';
import { CreateDialog } from './create-dialog';

export function NamedSettingsButton({
  disabled,
  tag,
}: NamedSettingsButtonProps) {
  const anchorRef = useRef<HTMLSpanElement | null>(null);

  const {
    openMenu,
    closeMenu,
    opened,
    savedItems,
    selectSaved,
    createSaved,
    deleteSaved,
    selectedItem,
    confirmDeleteDialog,
    createDialog,
  } = useNamedSettingsMenu(tag);

  const intl = useIntl();

  return (
    <>
      <TableToolbarButton
        ref={anchorRef}
        disabled={disabled}
        label={<SaveIcon />}
        onClick={openMenu}
        tooltip={intl.formatMessage({
          id: 'EAS_TABLE_SAVED_MENU_BTN',
          defaultMessage: 'Uložené nastavení',
        })}
      />
      <Menu
        getContentAnchorEl={null}
        anchorEl={anchorRef.current}
        keepMounted
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'left',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'left',
        }}
        MenuListProps={{
          disablePadding: true,
        }}
        open={opened}
        onClose={closeMenu}
      >
        {savedItems.map((saved, i) => (
          <NamedSettingsItem
            key={i}
            label={saved.name}
            selected={saved.id === selectedItem}
            closeMenu={closeMenu}
            onClick={() => selectSaved(saved)}
          />
        ))}
        <Divider />
        <MenuItem
          disabled={selectedItem === null}
          onClick={() => {
            confirmDeleteDialog.current?.open();
            closeMenu();
          }}
        >
          <>
            <FormattedMessage
              id="EAS_TABLE_SAVED_MENU_DELETE"
              defaultMessage="Smazat nastavení"
            />
          </>
        </MenuItem>
        <MenuItem
          onClick={() => {
            createDialog.current?.open();
            closeMenu();
          }}
        >
          <>
            <FormattedMessage
              id="EAS_TABLE_SAVED_MENU_CREATE"
              defaultMessage="Uložit nastavení"
            />
          </>
        </MenuItem>
      </Menu>
      <ConfirmDialog
        ref={confirmDeleteDialog}
        onConfirm={() => deleteSaved(selectedItem!)}
        onCancel={noop}
        title={
          <FormattedMessage
            id="EAS_TABLE_SAVED_REMOVE_DIALOG_TITLE"
            defaultMessage="Varování"
          />
        }
        text={
          <FormattedMessage
            id="EAS_TABLE_SAVED_REMOVE_DIALOG_TEXT"
            defaultMessage="Opravdu chcete smazat uložené nastavení ?"
          />
        }
      />
      <CreateDialog onConfirm={createSaved} ref={createDialog} />
    </>
  );
}
