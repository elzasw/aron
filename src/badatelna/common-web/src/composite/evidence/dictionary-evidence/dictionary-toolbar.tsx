import React, { useContext } from 'react';
import { FormattedMessage } from 'react-intl';
import ButtonGroup from '@material-ui/core/ButtonGroup/ButtonGroup';
import { DictionaryObject } from 'common/common-types';
import { DetailHandle } from 'composite/detail/detail-types';
import { DetailToolbarButton } from 'composite/detail/detail-toolbar-button';
import { DetailContext } from 'composite/detail/detail-context';
import { useStyles } from 'composite/detail/detail-styles';
import { useDictionary } from './dictionary-hook';

export function DictionaryToolbar() {
  const classes = useStyles();
  const { isExisting, source, onPersisted } = useContext<
    DetailHandle<DictionaryObject>
  >(DetailContext);

  const { activate, deactivate } = useDictionary({ source, onPersisted });

  return (
    <>
      {isExisting && (
        <ButtonGroup
          size="small"
          variant="outlined"
          className={classes.toolbarIndentLeft}
        >
          {source.data?.active ? (
            <DetailToolbarButton
              label={
                <FormattedMessage
                  id="EAS_DICTIONARY_EVIDENCE_TOOLBAR_BTN_DEACTIVATE"
                  defaultMessage="Deaktivovat"
                />
              }
              tooltip={
                <FormattedMessage
                  id="EAS_DICTIONARY_EVIDENCE_TOOLBAR_TOOLTIP_DEACTIVATE"
                  defaultMessage="Záznam číselníku se vyřadí ze seznamu použitelných hodnot v evidencích"
                />
              }
              onClick={deactivate}
            />
          ) : (
            <DetailToolbarButton
              label={
                <FormattedMessage
                  id="EAS_DICTIONARY_EVIDENCE_TOOLBAR_BTN_ACTIVATE"
                  defaultMessage="Aktivovat"
                />
              }
              tooltip={
                <FormattedMessage
                  id="EAS_DICTIONARY_EVIDENCE_TOOLBAR_TOOLTIP_ACTIVATE"
                  defaultMessage="Záznam číselníku se přidá do seznamu použitelných hodnot v evidencích"
                />
              }
              onClick={activate}
            />
          )}
        </ButtonGroup>
      )}
    </>
  );
}
