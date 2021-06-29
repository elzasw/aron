import React, { forwardRef, useContext, useState } from 'react';
import { useIntl } from 'react-intl';
import Typography from '@material-ui/core/Typography';
import { useEventCallback } from 'utils/event-callback-hook';
import { useStaticListSource } from 'utils/list-source-hook';
import { Dialog } from 'components/dialog/dialog';
import { DialogHandle } from 'components/dialog/dialog-types';
import { Select } from 'components/select/select';
import { DashboardContext } from './dashboard-context';

export const DashboardAddDialog = forwardRef<DialogHandle, any>(
  // eslint-disable-next-line no-empty-pattern
  function DashboardAddDialog({}, ref) {
    const intl = useIntl();
    const [value, setValue] = useState<string | null>(null);

    const { loadDashboard, saveDashboard } = useContext(DashboardContext);

    const onConfirm = useEventCallback(() => {
      if (value != null) {
        const items = loadDashboard();
        saveDashboard([...items, value]);
      }
    });

    const onShow = useEventCallback(() => {
      setValue(null);
    });

    return (
      <Dialog
        ref={ref}
        title={intl.formatMessage({
          id: 'EAS_DASHBOARD_ADD_DIALOG_TITLE',
          defaultMessage: 'Přidat prvek',
        })}
        onConfirm={onConfirm}
        onShow={onShow}
      >
        {() => (
          <DashboardEditDialogContent
            value={value}
            onChange={setValue}
            selected={loadDashboard()}
          />
        )}
      </Dialog>
    );
  }
);

export function DashboardEditDialogContent({
  value,
  onChange,
  selected,
}: {
  value: string | null;
  onChange: (item: string | null) => void;
  selected: string[];
}) {
  const intl = useIntl();
  const { definitions } = useContext(DashboardContext);

  const filteredDefinitions = definitions.filter(
    (definition) =>
      definition.dashboardSupport && !selected.includes(definition.id)
  );

  const source = useStaticListSource(filteredDefinitions);

  return (
    <>
      <Typography>
        {intl.formatMessage({
          id: 'EAS_DASHBOARD_ADD_DIALOG_MSG',
          defaultMessage: 'Vyberte prvek, který chcete vložit na nástěnku',
        })}
      </Typography>
      <Select
        source={source}
        value={value}
        onChange={onChange as any}
        labelMapper={(o) => o.label}
        valueIsId
      />
    </>
  );
}
