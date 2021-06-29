import React, { RefObject } from 'react';
import { useIntl } from 'react-intl';
import Box from '@material-ui/core/Box';
import { FormHandle } from 'composite/form/form-types';
import { Button } from 'components/button/button';
import { useStyles } from '../reporting-styles';
import { ReportDefinition, Report } from '../reporting-types';
import { ReportSettingsForm } from './report-settings-form';

export function ReportInput({
  generate,
  exportClick,
  formRef,
  definition,
  exportTag,
  report,
}: {
  definition: ReportDefinition | undefined;
  report: Report | null;
  generate: () => Promise<void>;
  exportClick: () => void;
  formRef: RefObject<FormHandle<any>>;
  exportTag?: string;
}) {
  const intl = useIntl();
  const classes = useStyles();

  return (
    <>
      <div className={classes.parametersWrapper}>
        <ReportSettingsForm ref={formRef} definition={definition} />
        <br />
        <Box display="flex">
          <Button
            outlined
            label={intl.formatMessage({
              id: 'EAS_REPORTING_INPUT_BUTTON_GENERATE',
              defaultMessage: 'Generovat',
            })}
            onClick={generate}
            disabled={definition?.autogenerate || definition === undefined}
          />
          <Box width={10} />
          {exportTag != null && (
            <Button
              outlined
              label={intl.formatMessage({
                id: 'EAS_REPORTING_INPUT_BUTTON_EXPORT',
                defaultMessage: 'Exportovat',
              })}
              onClick={exportClick}
              disabled={definition === undefined || report === null}
            />
          )}
        </Box>
      </div>
    </>
  );
}
