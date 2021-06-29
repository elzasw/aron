import React, { useContext, ComponentType } from 'react';
import { useIntl } from 'react-intl';
import { DetailContext } from 'composite/detail/detail-context';
import {
  DetailHandle,
  DetailToolbarButtonType,
  DetailToolbarButtonProps,
} from 'composite/detail/detail-types';
import { Job } from '../schedule-types';
import { startCall, cancelCall } from './jobs-api';
import { DetailToolbarButtonAction } from 'composite/detail/detail-toolbar-button-action';
import { useEventCallback } from 'utils/event-callback-hook';
import { ScheduleContext } from '../schedule-context';

export function jobsToolbarFactory({
  ButtonComponent,
}: {
  ButtonComponent?: ComponentType<DetailToolbarButtonProps>;
}) {
  return function JobsToolbar() {
    const intl = useIntl();
    const { jobUrl } = useContext(ScheduleContext);
    const { isExisting, source } = useContext<DetailHandle<Job>>(DetailContext);

    const running = source.data?.running;

    const start = useEventCallback((id: string) => {
      return startCall(jobUrl, id);
    });

    const cancel = useEventCallback((id: string) => {
      return cancelCall(jobUrl, id);
    });

    return (
      <>
        {isExisting && (
          <>
            {!running && (
              <DetailToolbarButtonAction
                ButtonComponent={ButtonComponent}
                buttonProps={{
                  type: DetailToolbarButtonType.PRIMARY,
                }}
                promptKey="MAIL_CAMPAIGN_CHECK"
                apiCall={start}
                buttonLabel={intl.formatMessage({
                  id: 'EAS_SCHEDULE_JOBS_TOOLBAR_BTN_START',
                  defaultMessage: 'Spustit',
                })}
                buttonTooltip={intl.formatMessage({
                  id: 'EAS_SCHEDULE_JOBS_TOOLBAR_TOOLTIP_START',
                  defaultMessage: 'Otevrě dialog pro potvrzení spustění úlohy',
                })}
                dialogTitle={intl.formatMessage({
                  id: 'EAS_SCHEDULE_JOBS_START_DIALOG_TITLE',
                  defaultMessage: 'Varování',
                })}
                dialogText={intl.formatMessage({
                  id: 'EAS_SCHEDULE_JOBS_START_DIALOG_TEXT',
                  defaultMessage: 'Skutečně chcete spustit úlohu?',
                })}
                successMessage={intl.formatMessage({
                  id: 'EAS_SCHEDULE_JOBS_MSG_START_SUCCESS',
                  defaultMessage: 'Úloha byla úspěšně spuštěna.',
                })}
                errorMessage={intl.formatMessage({
                  id: 'EAS_SCHEDULE_JOBS_MSG_START_ERROR',
                  defaultMessage: 'Nastala chyba.',
                })}
              />
            )}
            {running && (
              <DetailToolbarButtonAction
                ButtonComponent={ButtonComponent}
                buttonProps={{
                  type: DetailToolbarButtonType.PRIMARY,
                }}
                promptKey="MAIL_CAMPAIGN_CHECK"
                apiCall={cancel}
                buttonLabel={intl.formatMessage({
                  id: 'EAS_SCHEDULE_JOBS_TOOLBAR_BTN_CANCEL',
                  defaultMessage: 'Zastavit',
                })}
                buttonTooltip={intl.formatMessage({
                  id: 'EAS_SCHEDULE_JOBS_TOOLBAR_TOOLTIP_CANCEL',
                  defaultMessage: 'Otevrě dialog pro potvrzení zastavení úlohy',
                })}
                dialogTitle={intl.formatMessage({
                  id: 'EAS_SCHEDULE_JOBS_CANCEL_DIALOG_TITLE',
                  defaultMessage: 'Varování',
                })}
                dialogText={intl.formatMessage({
                  id: 'EAS_SCHEDULE_JOBS_CANCEL_DIALOG_TEXT',
                  defaultMessage:
                    'Zastavení běžící úlohy může spůsobit inkonzistenci dat. Skutečně chcete zastavit běžící úlohu?',
                })}
                successMessage={intl.formatMessage({
                  id: 'EAS_SCHEDULE_JOBS_MSG_CANCEL_SUCCESS',
                  defaultMessage: 'Úloha byla úspěšně zrušena.',
                })}
                errorMessage={intl.formatMessage({
                  id: 'EAS_SCHEDULE_JOBS_MSG_CANCEL_ERROR',
                  defaultMessage: 'Nastala chyba.',
                })}
              />
            )}
          </>
        )}
      </>
    );
  };
}
