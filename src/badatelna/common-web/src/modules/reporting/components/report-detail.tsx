import React, { ComponentType, RefAttributes } from 'react';
import { useIntl } from 'react-intl';
import CircularProgress from '@material-ui/core/CircularProgress';
import Box from '@material-ui/core/Box';
import { Panel } from 'components/panel/panel';
import { useStyles } from '../reporting-styles';
import { useReportData } from '../hook/data-hook';
import { ReportDefinition } from '../reporting-types';
import { ReportData } from './report-data';
import { ReportInput } from './report-input';
import { useReportExports } from '../hook/export-hook';
import { DialogHandle } from 'components/dialog/dialog-types';
import { ExportDialog } from 'modules/export/components/dialog/export-dialog/export-dialog';
import { ExportDialogProps } from 'modules/export/components/dialog/export-dialog/export-dialog-types';

export function ReportDetail({
  definition,
  ExportDialogComponent = ExportDialog,
  exportTag,
}: {
  definition: ReportDefinition | undefined;
  /**
   * Custom export dialog component.
   */
  ExportDialogComponent?: ComponentType<
    ExportDialogProps & RefAttributes<DialogHandle>
  >;
  exportTag?: string;
}) {
  const intl = useIntl();
  const classes = useStyles();

  const { generate, loading, source, formRef, resultRef } = useReportData({
    id: definition?.id,
  });

  const { exportDialogRef, openExportDialog, provideData } = useReportExports({
    report: source.data,
    definition,
  });

  return (
    <>
      <div className={classes.wrapperDetail}>
        {(source.loading || loading) && (
          <div className={classes.loaderWrapper}>
            <CircularProgress disableShrink className={classes.loader} />
          </div>
        )}
        <Box>
          <Panel
            label={intl.formatMessage(
              {
                id: 'EAS_REPORTING_DETAIL_PANEL_TITLE_REPORT',
                defaultMessage: 'Report {name}',
              },
              {
                name: definition !== undefined ? definition.label : '',
              }
            )}
            sideBorder={true}
            className={classes.titlePanel}
          />
        </Box>
        <Box>
          <Panel
            label={intl.formatMessage({
              id: 'EAS_REPORTING_DETAIL_PANEL_TITLE_PARAMETERS',
              defaultMessage: 'Parametry',
            })}
            sideBorder={true}
            className={classes.inputPanel}
          >
            <ReportInput
              definition={definition}
              report={source.data}
              generate={generate}
              exportClick={openExportDialog}
              exportTag={exportTag}
              formRef={formRef}
            />
          </Panel>
        </Box>
        <Box>
          <Panel
            label={intl.formatMessage({
              id: 'EAS_REPORTING_DETAIL_PANEL_TITLE_DATA',
              defaultMessage: 'Data',
            })}
            sideBorder={true}
            className={classes.dataPanel}
          >
            <ReportData
              key={source.data?.id}
              report={source.data}
              formRef={resultRef}
            />
          </Panel>
        </Box>
      </div>

      {exportTag != null && (
        <ExportDialogComponent
          ref={exportDialogRef}
          tag={exportTag}
          provideData={provideData}
        />
      )}
    </>
  );
}
