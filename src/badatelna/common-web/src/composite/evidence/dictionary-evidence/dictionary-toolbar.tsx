import React, { useContext } from 'react';
import { useIntl } from 'react-intl';
import ButtonGroup from '@material-ui/core/ButtonGroup/ButtonGroup';
import { DictionaryObject } from 'common/common-types';
import { DetailHandle, DetailMode } from 'composite/detail/detail-types';
import { activateItem, deactivateItem } from './dictionary-api';
import { DetailContext } from 'composite/detail/detail-context';
import { useStyles } from 'composite/detail/detail-styles';
import { DetailToolbarButtonAction } from 'composite/detail/detail-toolbar-button-action';
import { useEventCallback } from 'utils/event-callback-hook';

export function DictionaryToolbar() {
  const intl = useIntl();
  const classes = useStyles();
  const { source, mode } = useContext<DetailHandle<DictionaryObject>>(
    DetailContext
  );

  const activateCall = useEventCallback((id: string) => {
    return activateItem(source.url, id);
  });

  const deactivateCall = useEventCallback((id: string) => {
    return deactivateItem(source.url, id);
  });

  return (
    <>
      {mode === DetailMode.VIEW && (
        <ButtonGroup
          size="small"
          variant="outlined"
          className={classes.toolbarIndentLeft}
        >
          {source.data?.active ? (
            <DetailToolbarButtonAction
              promptKey="DICTIONARY_DEACTIVATE"
              buttonLabel={intl.formatMessage({
                id: 'EAS_DICTIONARY_EVIDENCE_TOOLBAR_BTN_DEACTIVATE',
                defaultMessage: 'Deaktivovat',
              })}
              buttonTooltip={intl.formatMessage({
                id: 'EAS_DICTIONARY_EVIDENCE_TOOLBAR_TOOLTIP_DEACTIVATE',
                defaultMessage: 'Otevře dialog s potvrzením akce',
              })}
              dialogTitle={intl.formatMessage({
                id: 'EAS_DICTIONARY_EVIDENCE_TOOLBAR_DIALOG_TITLE_DEACTIVATE',
                defaultMessage: 'Varování',
              })}
              dialogText={intl.formatMessage({
                id: 'EAS_DICTIONARY_EVIDENCE_TOOLBAR_DIALOG_TEXT_DEACTIVATE',
                defaultMessage:
                  'Záznam číselníku se vyřadí ze seznamu použitelných hodnot v evidencích',
              })}
              successMessage={intl.formatMessage({
                id: 'EAS_EVIDENCE_MSG_DEACTIVATED_SUCCESS',
                defaultMessage: 'Záznam byl úspěšně deaktivován.',
              })}
              errorMessage={intl.formatMessage({
                id: 'EAS_EVIDENCE_MSG_DEACTIVATED_ERROR',
                defaultMessage: 'Chyba volání funkce: {detail}',
              })}
              apiCall={deactivateCall}
            />
          ) : (
            <DetailToolbarButtonAction
              promptKey="DICTIONARY_ACTIVATE"
              buttonLabel={intl.formatMessage({
                id: 'EAS_DICTIONARY_EVIDENCE_TOOLBAR_BTN_ACTIVATE',
                defaultMessage: 'Aktivovat',
              })}
              buttonTooltip={intl.formatMessage({
                id: 'EAS_DICTIONARY_EVIDENCE_TOOLBAR_TOOLTIP_ACTIVATE',
                defaultMessage: 'Otevře dialog s potvrzením akce',
              })}
              dialogTitle={intl.formatMessage({
                id: 'EAS_DICTIONARY_EVIDENCE_TOOLBAR_DIALOG_TITLE_ACTIVATE',
                defaultMessage: 'Varování',
              })}
              dialogText={intl.formatMessage({
                id: 'EAS_DICTIONARY_EVIDENCE_TOOLBAR_DIALOG_TEXT_ACTIVATE',
                defaultMessage:
                  'Záznam číselníku se přidá do seznamu použitelných hodnot v evidencích',
              })}
              successMessage={intl.formatMessage({
                id: 'EAS_EVIDENCE_MSG_ACTIVATED_SUCCESS',
                defaultMessage: 'Záznam byl úspěšně aktivován.',
              })}
              errorMessage={intl.formatMessage({
                id: 'EAS_EVIDENCE_MSG_ACTIVATED_ERROR',
                defaultMessage: 'Chyba volání funkce: {detail}',
              })}
              apiCall={activateCall}
            />
          )}
        </ButtonGroup>
      )}
    </>
  );
}
