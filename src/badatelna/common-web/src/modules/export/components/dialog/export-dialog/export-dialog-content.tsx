import React, { forwardRef, useContext } from 'react';
import { FormattedMessage } from 'react-intl';
import { noop } from 'lodash';
import Grid from '@material-ui/core/Grid';
import RadioGroup from '@material-ui/core/RadioGroup';
import Radio from '@material-ui/core/Radio';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Button from '@material-ui/core/Button';
import CircularProgress from '@material-ui/core/CircularProgress';
import GetAppIcon from '@material-ui/icons/GetApp';
import Typography from '@material-ui/core/Typography';
import { DialogHandle } from 'components/dialog/dialog-types';
import { Dialog } from 'components/dialog/dialog';
import { Panel } from 'components/panel/panel';
import { TableField } from 'components/table-field/table-field';
import { composeRefs } from 'utils/compose-refs';
import { FilesContext } from 'common/files/files-context';
import { ExportContext } from 'modules/export/export-context';
import { ExportRequestState } from 'modules/export/export-types';
import { ExportDialogProps } from './export-dialog-types';
import { useStyles } from './export-dialog-styles';
import { useExportDialogHook } from './export-dialog-hook';
import { useAllowdExportTypes } from 'modules/export/export-api';

export const ExportDialogContent = forwardRef<DialogHandle, ExportDialogProps>(
  function ExportDialogContent(props, dialogRef) {
    const classes = useStyles();
    const { getFileUrl } = useContext(FilesContext);
    const { disableAsync, disableSync } = useContext(ExportContext);

    const {
      loading,
      ref,
      syncLoading,
      asyncLoading,
      selectedFormat,
      selectedTemplate,
      state,
      resultId,
      setSelectedFormat,
      selectTemplate,
      columns,
      templates,
      generateSync,
      generateASync,
      downloadSync,
    } = useExportDialogHook(props.tag, props.provideData);

    const composedRef = composeRefs(ref, dialogRef);

    const types = useAllowdExportTypes(selectedTemplate?.allowedTypes);

    return (
      <Dialog
        ref={composedRef}
        loading={loading}
        title={
          <FormattedMessage
            id="EAS_EXPORT_DIALOG_TITLE"
            defaultMessage="Tisk"
          />
        }
        showConfirm={false}
        actions={[
          !disableSync && state !== ExportRequestState.PROCESSED && (
            <Button
              key="generateSync"
              variant="outlined"
              color="primary"
              onClick={generateSync}
              disabled={loading}
              startIcon={
                syncLoading && <CircularProgress size="20px" color="inherit" />
              }
            >
              <Typography classes={{ root: classes.buttonLabel }}>
                <FormattedMessage
                  id="EAS_EXPORT_DIALOG_BTN_GENERATE_SYNC"
                  defaultMessage="Tisknout"
                />
              </Typography>
            </Button>
          ),
          state === ExportRequestState.PROCESSED && (
            <Button
              key="downloadSync"
              variant="outlined"
              color="primary"
              onClick={downloadSync}
              disabled={loading}
              startIcon={<GetAppIcon />}
              href={getFileUrl(resultId ?? '')}
            >
              <Typography classes={{ root: classes.buttonLabel }}>
                <FormattedMessage
                  id="EAS_EXPORT_DIALOG_BTN_DOWNLOAD"
                  defaultMessage="Stáhnout"
                />
              </Typography>
            </Button>
          ),
          !disableAsync && (
            <Button
              key="generateAsync"
              variant="outlined"
              onClick={generateASync}
              color="secondary"
              disabled={loading}
              startIcon={
                asyncLoading && <CircularProgress size="20px" color="inherit" />
              }
            >
              <Typography classes={{ root: classes.buttonLabel }}>
                <FormattedMessage
                  id="EAS_EXPORT_DIALOG_BTN_GENERATE_ASYNC"
                  defaultMessage="Zařadit do fronty"
                />
              </Typography>
            </Button>
          ),
        ]}
      >
        {() => (
          <div className={classes.dialogWrapper}>
            <Grid container justify="center" spacing={1}>
              <Grid item xs={9}>
                <Panel
                  label={
                    <FormattedMessage
                      id="EAS_EXPORT_DIALOG_PANEL_TEMPLATE"
                      defaultMessage="Šablona"
                    />
                  }
                >
                  <TableField
                    maxRows={10}
                    showToolbar={false}
                    value={templates}
                    columns={columns}
                    onChange={noop}
                    onSelect={selectTemplate}
                  />
                </Panel>
              </Grid>
              <Grid item xs={3}>
                <Panel
                  label={
                    <FormattedMessage
                      id="EAS_EXPORT_DIALOG_PANEL_FORMAT"
                      defaultMessage="Formát"
                    />
                  }
                >
                  <RadioGroup
                    aria-label="format"
                    value={selectedFormat}
                    onChange={(_, value) => setSelectedFormat(value as any)}
                  >
                    {types.items.map((type) => (
                      <FormControlLabel
                        key={type.id}
                        value={type.id}
                        control={
                          <Radio
                            color="primary"
                            classes={{ root: classes.radioRoot }}
                          />
                        }
                        label={type.name}
                        classes={{
                          label: classes.labelLabel,
                          root: classes.labelRoot,
                        }}
                        className={classes.radioControl}
                      />
                    ))}
                  </RadioGroup>
                </Panel>
              </Grid>
            </Grid>
          </div>
        )}
      </Dialog>
    );
  }
);
