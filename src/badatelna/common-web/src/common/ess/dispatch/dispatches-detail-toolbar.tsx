import React, { useContext, useMemo } from 'react';
import { useIntl } from 'react-intl';
import ButtonGroup from '@material-ui/core/ButtonGroup/ButtonGroup';
import { useStyles } from './dispatches-styles';
import { DetailHandle } from 'composite/detail/detail-types';
import { DetailContext } from 'composite/detail/detail-context';
import { DetailToolbarButtonAction } from 'composite/detail/detail-toolbar-button-action';
import { sendFactory, deliverFactory } from './dispatches-api';
import { Dispatch, DispatchState } from '../ess-types';

export function DispatchToolbar({ url }: { url: string }) {
  const intl = useIntl();
  const classes = useStyles();
  const { source } = useContext<DetailHandle<Dispatch>>(DetailContext);

  const send = useMemo(() => sendFactory(url), [url]);
  const deliver = useMemo(() => deliverFactory(url), [url]);

  return (
    <>
      <>
        <ButtonGroup
          size="small"
          variant="outlined"
          className={classes.toolbarIndentLeft}
        >
          {source.data?.state === DispatchState.CREATED && (
            <DetailToolbarButtonAction
              promptKey="ESS_DISPATCH_SEND"
              apiCall={send}
              buttonLabel={intl.formatMessage({
                id: 'ESS_DISPATCH_TOOLBAR_BTN_SEND',
                defaultMessage: 'Odeslat',
              })}
              buttonTooltip={intl.formatMessage({
                id: 'ESS_DISPATCH_TOOLBAR_TOOLTIP_SEND',
                defaultMessage: 'Otevrě dialog s ručním odesláním',
              })}
              dialogTitle={intl.formatMessage({
                id: 'ESS_DISPATCH_SEND_DIALOG_TITLE',
                defaultMessage: 'Varování',
              })}
              dialogText={intl.formatMessage({
                id: 'ESS_DISPATCH_SEND_DIALOG_TEXT',
                defaultMessage:
                  'Skutečně chcete označit vypravení za odeslané ?',
              })}
            />
          )}
          {source.data?.state === DispatchState.SENT && (
            <DetailToolbarButtonAction
              promptKey="ESS_DISPATCH_DELIVER"
              apiCall={deliver}
              buttonLabel={intl.formatMessage({
                id: 'ESS_DISPATCH_TOOLBAR_BTN_DELIVER',
                defaultMessage: 'Doručit',
              })}
              buttonTooltip={intl.formatMessage({
                id: 'ESS_DISPATCH_TOOLBAR_TOOLTIP_DELIVER',
                defaultMessage: 'Otevrě dialog s ručním doručením',
              })}
              dialogTitle={intl.formatMessage({
                id: 'ESS_DISPATCH_DELIVER_DIALOG_TITLE',
                defaultMessage: 'Varování',
              })}
              dialogText={intl.formatMessage({
                id: 'ESS_DISPATCH_DELIVER_DIALOG_TEXT',
                defaultMessage:
                  'Skutečně chcete označit vypravení za doručené ?',
              })}
            />
          )}
        </ButtonGroup>
      </>
    </>
  );
}
