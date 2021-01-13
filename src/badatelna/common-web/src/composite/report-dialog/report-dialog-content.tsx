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
import { useStyles } from './report-dialog-styles';
import { useReportTypes } from './report-dialog-api';
import { ReportDialogProps } from './report-dialog-types';
import { useReportDialogHook } from './report-dialog-hook';
import { ReportRequestState } from 'common/common-types';
import { composeRefs } from 'utils/compose-refs';
import { FilesContext } from 'common/files/files-context';

export const ReportDialogContent = forwardRef<DialogHandle, ReportDialogProps>(
  function ReportDialogContent(props, dialogRef) {
    const classes = useStyles();
    const { getFileUrl } = useContext(FilesContext);

    const {
      loading,
      ref,
      syncLoading,
      asyncLoading,
      selectedFormat,
      state,
      resultId,
      setSelectedFormat,
      selectTemplate,
      columns,
      templates,
      generateSync,
      generateASync,
      downloadSync,
    } = useReportDialogHook(props.tag, props.provideData);

    const composedRef = composeRefs(ref, dialogRef);

    const types = useReportTypes();

    return (
      <Dialog
        ref={composedRef}
        loading={loading}
        title={
          <FormattedMessage
            id="EAS_REPORT_DIALOG_TITLE"
            defaultMessage="Tisk"
          />
        }
        showConfirm={false}
        actions={[
          state !== ReportRequestState.PROCESSED && (
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
                  id="EAS_REPORT_DIALOG_BTN_GENERATE_SYNC"
                  defaultMessage="Tisknout"
                />
              </Typography>
            </Button>
          ),
          state === ReportRequestState.PROCESSED && (
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
                  id="EAS_REPORT_DIALOG_BTN_DOWNLOAD"
                  defaultMessage="Stáhnout"
                />
              </Typography>
            </Button>
          ),
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
                id="EAS_REPORT_DIALOG_BTN_GENERATE_ASYNC"
                defaultMessage="Zařadit do fronty"
              />
            </Typography>
          </Button>,
        ]}
      >
        {() => (
          <div className={classes.dialogWrapper}>
            <Grid container justify="center" spacing={1}>
              <Grid item xs={9}>
                <Panel
                  label={
                    <FormattedMessage
                      id="EAS_REPORT_DIALOG_PANEL_TEMPLATE"
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
                      id="EAS_REPORT_DIALOG_PANEL_FORMAT"
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
