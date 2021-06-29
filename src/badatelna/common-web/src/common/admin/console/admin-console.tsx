import React from 'react';
import Grid from '@material-ui/core/Grid';
import Box from '@material-ui/core/Box';
import { Form } from 'composite/form/form';
import { FormPanel } from 'composite/form/fields/form-panel';
import { FormTextArea } from 'composite/form/fields/form-text-area';
import { useAdminConsole } from './admin-console-hook';
import { FormattedMessage, useIntl } from 'react-intl';
import { AdminConsoleInputFields } from './admin-console-input-fields';

export function AdminConsole() {
  const intl = useIntl();
  const { handleSubmit, handleClear, resultForm, loading } = useAdminConsole();

  return (
    <>
      <Box margin={2}>
        <Grid container justify="space-around" spacing={2}>
          <Grid item xs={6}>
            <h1>
              <FormattedMessage
                id="EAS_ADMIN_CONSOLE_INPUT_TITLE"
                defaultMessage="Vstup"
              />
            </h1>
            <Form
              editing={true}
              initialValues={{} as any}
              onSubmit={handleSubmit}
            >
              <AdminConsoleInputFields
                loading={loading}
                handleClear={handleClear}
              />
            </Form>
          </Grid>
          <Grid item xs={6}>
            <h1>
              <FormattedMessage
                id="EAS_ADMIN_CONSOLE_OUTPUT_TITLE"
                defaultMessage="Výstup"
              />
            </h1>
            <Form
              editing={false}
              initialValues={{ result: '', console: '', log: '' }}
              // eslint-disable-next-line @typescript-eslint/no-empty-function
              onSubmit={() => {}}
              ref={resultForm}
            >
              <FormPanel
                label={intl.formatMessage({
                  id: 'EAS_ADMIN_CONSOLE_LABEL_RESULT',
                  defaultMessage: 'Výsledek',
                })}
              >
                <FormTextArea name="result" labelOptions={{ hide: true }} />
              </FormPanel>
              <FormPanel
                label={intl.formatMessage({
                  id: 'EAS_ADMIN_CONSOLE_LABEL_CONSOLE',
                  defaultMessage: 'Konzole',
                })}
              >
                <FormTextArea
                  name="console"
                  minRows={7}
                  labelOptions={{ hide: true }}
                />
              </FormPanel>
              <FormPanel
                label={intl.formatMessage({
                  id: 'EAS_ADMIN_CONSOLE_LABEL_LOG',
                  defaultMessage: 'Log',
                })}
              >
                <FormTextArea
                  name="log"
                  minRows={10}
                  labelOptions={{ hide: true }}
                />
              </FormPanel>
            </Form>
          </Grid>
        </Grid>
      </Box>
    </>
  );
}
