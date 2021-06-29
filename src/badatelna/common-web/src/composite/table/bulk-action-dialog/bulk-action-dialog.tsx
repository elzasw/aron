import React, { forwardRef, ComponentType } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { noop } from 'lodash';
import Grid from '@material-ui/core/Grid';
import Typography from '@material-ui/core/Typography';
import { Dialog } from 'components/dialog/dialog';
import { Form } from 'composite/form/form';
import { FormCustomField } from 'composite/form/fields/form-custom-field';
import { useBulkActionDialog } from './bulk-action-dialog-hook';
import {
  BulkActionDialogHandle,
  BulkActionFieldsProps,
} from './bulk-action-dialog-types';
import { useStyles } from './bulk-action-dialog-styles';
import { Filter } from 'common/common-types';
import { ResultStatus, bulkActionDialogApi } from './bulk-action-dialog-api';

export function bulkActionDialogFactory<FORM_DATA>({
  url,
  paramsMapper,
  title,
  Fields,
  apiCall = bulkActionDialogApi,
}: {
  url: string;
  paramsMapper: (formData?: FORM_DATA) => Record<string, any>;
  title: string;
  Fields: ComponentType<BulkActionFieldsProps>;
  apiCall: (
    url: string,
    filters: Filter[],
    ids: string[],
    params?: Record<string, any>
  ) => Promise<ResultStatus>;
}) {
  return forwardRef(function BulkActionDialogWithoutRef(
    _,
    ref: React.Ref<BulkActionDialogHandle>
  ) {
    const {
      submitting,
      dialogRef,
      formRef,
      applyToSelected,
      count,
      confirm,
    } = useBulkActionDialog<FORM_DATA>({
      url,
      paramsMapper,
      ref,
      apiCall,
    });

    const classes = useStyles();

    const intl = useIntl();

    return (
      <Dialog
        title={title}
        confirmLabel={intl.formatMessage({
          id: 'EAS_TABLE_BULK_DIALOG_BTN_CONFIRM',
          defaultMessage: 'Vykonat',
        })}
        onConfirm={confirm}
        ref={dialogRef}
      >
        {() => (
          <Grid container>
            <div style={{ width: 400 }}>
              <Form
                initialValues={{} as any}
                onSubmit={noop}
                ref={formRef}
                editing={true}
              >
                <FormCustomField
                  labelOptions={{ hide: true }}
                  layoutOptions={{ noUnderline: true }}
                >
                  <Typography
                    variant="body1"
                    classes={{ root: classes.warning }}
                  >
                    <FormattedMessage
                      id="EAS_TABLE_BULK_DIALOG_WARNING"
                      defaultMessage={`Akce bude aplikována na {count, plural, 
                      one {1 {applyToSelected, select, 
                          true {VYBRANOU}
                          false {VYFILTROVANOU}
                        } položku}
                      few {# {applyToSelected, select, 
                          true {VYBRANÉ}
                          false {VYFILTROVANÉ}
                        } položky}
                      other {# {applyToSelected, select, 
                          true {VYBRANÝCH}
                          false {VYFILTROVANÝCH}
                        } položek}
                      }.`}
                      values={{ count, applyToSelected }}
                    />
                  </Typography>
                </FormCustomField>
                <Fields disabled={submitting} />
              </Form>
            </div>
          </Grid>
        )}
      </Dialog>
    );
  });
}
