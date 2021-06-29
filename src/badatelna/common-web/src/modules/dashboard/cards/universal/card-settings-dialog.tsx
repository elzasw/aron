import React, { forwardRef, useRef, useContext } from 'react';
import { useIntl } from 'react-intl';
import Box from '@material-ui/core/Box';
import { useEventCallback } from 'utils/event-callback-hook';
import { DialogHandle } from 'components/dialog/dialog-types';
import { Dialog } from 'components/dialog/dialog';
import { FormHandle } from 'composite/form/form-types';
import { ReportingContext } from 'modules/reporting/reporting-context';
import { ReportDefinition } from 'modules/reporting/reporting-types';
import { ReportSettingsForm } from 'modules/reporting/components/report-settings-form';
import { CardSettingsDialogProps } from '../../dashboard-types';

export const CardSettingsDialog = forwardRef<
  DialogHandle,
  CardSettingsDialogProps
>(
  // eslint-disable-next-line no-empty-pattern
  function CardSettingsDialog({ report, definition, load }, ref) {
    const intl = useIntl();
    const { generate } = useContext(ReportingContext);
    const formRef = useRef<FormHandle<any>>(null);

    const onConfirm = useEventCallback(async () => {
      await generate(definition.id, formRef.current?.getFieldValues());
      await load();
    });

    const onShown = useEventCallback(() => {
      formRef.current?.setFieldValues(report.configuration);
    });

    return (
      <Dialog
        ref={ref}
        title={intl.formatMessage({
          id: 'EAS_DASHBOARD_CARD_SETTINGS_DIALOG_TITLE',
          defaultMessage: 'Nastavení prvku',
        })}
        onConfirm={onConfirm}
        onShown={onShown}
      >
        {() => (
          <Box width={800}>
            <CardSettingsDialogContent
              formRef={formRef}
              definition={definition}
            />
          </Box>
        )}
      </Dialog>
    );
  }
);

export function CardSettingsDialogContent({
  definition,
  formRef,
}: {
  definition: ReportDefinition;
  formRef: React.RefObject<FormHandle<any>>;
}) {
  return <ReportSettingsForm ref={formRef} definition={definition} />;
}
