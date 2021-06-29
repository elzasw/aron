import React from 'react';
import Grid from '@material-ui/core/Grid';
import Box from '@material-ui/core/Box';
import CircularProgress from '@material-ui/core/CircularProgress';
import { FormCheckbox } from 'composite/form/fields/form-checkbox';
import { FormEditor } from 'composite/form/fields/form-editor';
import { FormPanel } from 'composite/form/fields/form-panel';
import { FormSubmitButton } from 'composite/form/fields/form-submit-button';
import { FormSelect } from 'composite/form/fields/form-select';
import { useScriptType } from './admin-console-api';
import { FormattedMessage, useIntl } from 'react-intl';
import { useFormSelector } from 'composite/form/selectors/selector';
import { AdminConsoleInput } from './admin-console-types';
import { ScriptType } from 'common/common-types';

export function AdminConsoleInputFields({
  loading,
  handleClear,
}: {
  loading: boolean;
  handleClear: () => void;
}) {
  const intl = useIntl();
  const scriptTypes = useScriptType();

  const scriptType = useFormSelector(
    (data: AdminConsoleInput) => data.scriptType
  );

  return (
    <>
      <Grid container spacing={2}>
        <Grid item xs={6}>
          <FormPanel
            label={intl.formatMessage({
              id: 'EAS_ADMIN_CONSOLE_LABEL_SCRIPT_LANGUAGE',
              defaultMessage: 'Skriptovací jazyk',
            })}
          >
            <FormSelect
              name="scriptType"
              required
              labelOptions={{ hide: true }}
              source={scriptTypes}
              tooltipMapper={(o) => o.name}
              valueIsId={true}
            />
          </FormPanel>
        </Grid>
        <Grid item xs={6}>
          <FormPanel
            label={intl.formatMessage({
              id: 'EAS_ADMIN_CONSOLE_LABEL_USE_TRANSACTION',
              defaultMessage: 'Vykonat v transakci',
            })}
          >
            <FormCheckbox name="useTransaction" labelOptions={{ hide: true }} />
          </FormPanel>
        </Grid>
      </Grid>

      <FormPanel
        label={intl.formatMessage({
          id: 'EAS_ADMIN_CONSOLE_LABEL_SCRIPT',
          defaultMessage: 'Skript',
        })}
      >
        <FormEditor
          name="script"
          language={
            scriptType === ScriptType.JAVASCRIPT ? 'javascript' : 'groovy'
          }
          labelOptions={{ hide: true }}
          height={300}
        />
      </FormPanel>

      <Grid container>
        <Grid item xs={6}>
          <FormSubmitButton
            type="submit"
            variant="outlined"
            color="primary"
            disabled={scriptType == null || loading}
            startIcon={
              loading && <CircularProgress size="20px" color="inherit" />
            }
          >
            <FormattedMessage
              id="EAS_ADMIN_CONSOLE_BTN_SUBMIT"
              defaultMessage="Vykonat"
            />
          </FormSubmitButton>
          <Box width={10} display="inline-block" />
          <FormSubmitButton variant="outlined" onClick={handleClear}>
            <FormattedMessage
              id="EAS_ADMIN_CONSOLE_BTN_RESET"
              defaultMessage="Smazat"
            />
          </FormSubmitButton>
        </Grid>
      </Grid>
    </>
  );
}
