import React, { useContext } from 'react';
import { FormattedMessage } from 'react-intl';
import { noop } from 'lodash';
import ButtonGroup from '@material-ui/core/ButtonGroup/ButtonGroup';
import { DetailContext } from 'composite/detail/detail-context';
import { DetailToolbarButton } from 'composite/detail/detail-toolbar-button';
import { ConfirmDialog } from 'composite/confirm-dialog/confirm-dialog';
import { DetailHandle } from 'composite/detail/detail-types';
import { Job } from '../schedule-types';
import { useStyles } from './jobs-toolbar-styles';
import { useScheduleJobsToolbar } from './jobs-toolbar-hook';

export function JobsToolbar() {
  const classes = useStyles();
  const { isExisting, source, onPersisted } = useContext<DetailHandle<Job>>(
    DetailContext
  );

  const {
    startDialogRef,
    cancelDialogRef,
    openStartDialog,
    openCancelDialog,
    start,
    cancel,
  } = useScheduleJobsToolbar({
    source,
    onPersisted,
  });

  const running = source.data?.running;

  return (
    <>
      {isExisting && (
        <>
          <ButtonGroup
            size="small"
            variant="outlined"
            className={classes.toolbarIndentLeft}
          >
            {!running && (
              <DetailToolbarButton
                label={
                  <FormattedMessage
                    id="EAS_SCHEDULE_JOBS_TOOLBAR_BTN_START"
                    defaultMessage="Spustit"
                  />
                }
                tooltip={
                  <FormattedMessage
                    id="EAS_SCHEDULE_JOBS_TOOLBAR_TOOLTIP_START"
                    defaultMessage="Otevrě dialog pro potvrzení spustění úlohy"
                  />
                }
                onClick={openStartDialog}
              />
            )}
            {running && (
              <DetailToolbarButton
                label={
                  <FormattedMessage
                    id="EAS_SCHEDULE_JOBS_TOOLBAR_BTN_CANCEL"
                    defaultMessage="Zastavit"
                  />
                }
                tooltip={
                  <FormattedMessage
                    id="EAS_SCHEDULE_JOBS_TOOLBAR_TOOLTIP_CANCEL"
                    defaultMessage="Otevrě dialog pro potvrzení zastavení úlohy"
                  />
                }
                onClick={openCancelDialog}
              />
            )}
          </ButtonGroup>
          <ConfirmDialog
            ref={startDialogRef}
            onConfirm={start}
            onCancel={noop}
            title={
              <FormattedMessage
                id="EAS_SCHEDULE_JOBS_START_DIALOG_TITLE"
                defaultMessage="Varování"
              />
            }
            text={
              <FormattedMessage
                id="EAS_SCHEDULE_JOBS_START_DIALOG_TEXT"
                defaultMessage="Skutečně chcete spustit úlohu?"
              />
            }
          />
          <ConfirmDialog
            ref={cancelDialogRef}
            onConfirm={cancel}
            onCancel={noop}
            title={
              <FormattedMessage
                id="EAS_SCHEDULE_JOBS_CANCEL_DIALOG_TITLE"
                defaultMessage="Varování"
              />
            }
            text={
              <FormattedMessage
                id="EAS_SCHEDULE_JOBS_CANCEL_DIALOG_TEXT"
                defaultMessage="Zastavení běžící úlohy může spůsobit inkonzistenci dat. Skutečně chcete zastavit běžící úlohu?"
              />
            }
          />
        </>
      )}
    </>
  );
}
