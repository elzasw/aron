import React, { useContext, useRef } from 'react';
import { noop, stubTrue, stubFalse } from 'lodash';
import { FormattedMessage } from 'react-intl';
import ButtonGroup from '@material-ui/core/ButtonGroup/ButtonGroup';
import RefreshIcon from '@material-ui/icons/Refresh';
import PlaylistAddCheckIcon from '@material-ui/icons/PlaylistAddCheck';
import AddBoxOutlinedIcon from '@material-ui/icons/AddBoxOutlined';
import EditOutlinedIcon from '@material-ui/icons/EditOutlined';
import DeleteOutlinedIcon from '@material-ui/icons/DeleteOutlined';
import { useStyles } from './detail-styles';
import { DetailToolbarButton } from './detail-toolbar-button';
import { DetailContext } from './detail-context';
import {
  DetailToolbarButtonType,
  DetailMode,
  DetailToolbarProps,
  DetailHandle,
} from './detail-types';
import { ConfirmDialog } from 'composite/confirm-dialog/confirm-dialog';
import { DialogHandle } from 'components/dialog/dialog-types';
import { useEventCallback } from 'utils/event-callback-hook';
import { DomainObject } from 'common/common-types';

export function DetailToolbar<OBJECT extends DomainObject>({
  before,
  after,
  showButton = stubTrue,
  disableButton = stubFalse,
}: DetailToolbarProps<OBJECT>) {
  const classes = useStyles();
  const { mode, source } = useContext<DetailHandle<OBJECT>>(DetailContext);
  const confirmDeleteDialog = useRef<DialogHandle>(null);

  const {
    refresh,
    startNew,
    startEditing,
    cancelEditing,
    validate,
    del,
    save,
  } = useContext(DetailContext);

  const handleDelete = useEventCallback(() => {
    confirmDeleteDialog.current?.open();
  });

  const handleNew = useEventCallback(() => {
    startNew();
  });

  return (
    <>
      <div className={classes.toolbarWrapper}>
        {before}
        {mode !== DetailMode.NONE && (
          <ButtonGroup
            size="small"
            variant="outlined"
            className={classes.toolbarIndentLeft}
          >
            {mode === DetailMode.VIEW && (
              <DetailToolbarButton
                label={<RefreshIcon />}
                tooltip={
                  <FormattedMessage
                    id="EAS_DETAIL_TOOLTIP_REFRESH"
                    defaultMessage="Obnovit data formuláře"
                  />
                }
                onClick={refresh}
              />
            )}
            <DetailToolbarButton
              label={<PlaylistAddCheckIcon />}
              tooltip={
                <FormattedMessage
                  id="EAS_DETAIL_TOOLTIP_VALIDATE"
                  defaultMessage="Zkontroluje vyplnění polí a další omezení."
                />
              }
              onClick={validate}
            />
          </ButtonGroup>
        )}

        <ButtonGroup
          size="small"
          variant="outlined"
          className={classes.toolbarIndentLeft}
        >
          {showButton({
            button: 'NEW',
            source,
          }) &&
            (mode === DetailMode.NONE || mode === DetailMode.VIEW) && (
              <DetailToolbarButton
                label={
                  <FormattedMessage
                    id="EAS_DETAIL_BTN_CREATE"
                    defaultMessage="Nový"
                  />
                }
                startIcon={<AddBoxOutlinedIcon />}
                tooltip={
                  <FormattedMessage
                    id="EAS_DETAIL_TOOLTIP_CREATE"
                    defaultMessage="Zapne editační režim s prázdným formulářem"
                  />
                }
                disabled={disableButton({
                  button: 'NEW',
                  source,
                })}
                onClick={handleNew}
              />
            )}
          {showButton({
            button: 'EDIT',
            source,
          }) &&
            mode === DetailMode.VIEW && (
              <DetailToolbarButton
                startIcon={<EditOutlinedIcon />}
                label={
                  <FormattedMessage
                    id="EAS_DETAIL_BTN_EDIT"
                    defaultMessage="Oprava"
                  />
                }
                tooltip={
                  <FormattedMessage
                    id="EAS_DETAIL_TOOLTIP_EDIT"
                    defaultMessage="Zapne editační režim s otevřeným záznamem"
                  />
                }
                disabled={disableButton({
                  button: 'EDIT',
                  source,
                })}
                onClick={startEditing}
              />
            )}
          {showButton({
            button: 'REMOVE',
            source,
          }) &&
            mode === DetailMode.VIEW && (
              <DetailToolbarButton
                startIcon={<DeleteOutlinedIcon />}
                label={
                  <FormattedMessage
                    id="EAS_DETAIL_BTN_DELETE"
                    defaultMessage="Smazat"
                  />
                }
                tooltip={
                  <FormattedMessage
                    id="EAS_DETAIL_TOOLTIP_DELETE"
                    defaultMessage="Nenávratně smaže záznam"
                  />
                }
                disabled={disableButton({
                  button: 'REMOVE',
                  source,
                })}
                onClick={handleDelete}
                type={DetailToolbarButtonType.SECONDARY}
              />
            )}
          {(mode === DetailMode.NEW || mode === DetailMode.EDIT) && (
            <DetailToolbarButton
              label={
                <FormattedMessage
                  id="EAS_DETAIL_BTN_SAVE"
                  defaultMessage="Uložit"
                />
              }
              tooltip={
                <FormattedMessage
                  id="EAS_DETAIL_TOOLTIP_SAVE"
                  defaultMessage="Uloží záznam do evidence"
                />
              }
              onClick={save}
              type={DetailToolbarButtonType.PRIMARY}
            />
          )}
          {(mode === DetailMode.NEW || mode === DetailMode.EDIT) && (
            <DetailToolbarButton
              label={
                <FormattedMessage
                  id="EAS_DETAIL_BTN_CANCEL"
                  defaultMessage="Zrušit"
                />
              }
              tooltip={
                <FormattedMessage
                  id="EAS_DETAIL_TOOLTIP_CANCEL"
                  defaultMessage="Přepne do prohlížecího režimu a všechny změny budou zahozeny"
                />
              }
              onClick={cancelEditing}
            />
          )}
        </ButtonGroup>
        {after}
      </div>
      <ConfirmDialog
        ref={confirmDeleteDialog}
        onConfirm={del}
        onCancel={noop}
        title={
          <FormattedMessage
            id="EAS_DETAIL_REMOVE_DIALOG_TITLE"
            defaultMessage="Varování"
          />
        }
        text={
          <FormattedMessage
            id="EAS_DETAIL_REMOVE_DIALOG_TEXT"
            defaultMessage="Opravdu chcete smazat záznam ?"
          />
        }
      />
    </>
  );
}
