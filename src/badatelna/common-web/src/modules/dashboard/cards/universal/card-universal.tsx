import React, { useContext, useRef } from 'react';
import { useIntl } from 'react-intl';
import clsx from 'clsx';
import { get } from 'lodash';
import Card from '@material-ui/core/Card';
import CardContent from '@material-ui/core/CardContent';
import Typography from '@material-ui/core/Typography';
import Box from '@material-ui/core/Box';
import IconButton from '@material-ui/core/IconButton';
import SettingsIcon from '@material-ui/icons/Settings';
import RefreshIcon from '@material-ui/icons/Refresh';
import CloseIcon from '@material-ui/icons/Close';
import { useEventCallback } from 'utils/event-callback-hook';
import { DialogHandle } from 'components/dialog/dialog-types';
import { ReportingContext } from 'modules/reporting/reporting-context';
import { CardProps } from '../../dashboard-types';
import { DashboardContext } from '../../dashboard-context';
import { CardAction } from '../action/card-action';
import { useStyles } from '../card-styles';
import { CardSettingsDialog } from './card-settings-dialog';

export function CardUniversal({
  report,
  load,
  loading,
  definitionId,
}: CardProps) {
  const intl = useIntl();
  const classes = useStyles();

  const { generate } = useContext(ReportingContext);
  const { definitions, remove, classes: classesOverride } = useContext(
    DashboardContext
  );

  const settingsDialogRef = useRef<DialogHandle>(null);

  const definition = definitions.find((d) => d.id === definitionId);

  const handleRemove = useEventCallback(() => {
    remove(definitionId);
  });

  const handleOpenSettingsDialog = useEventCallback(() => {
    settingsDialogRef.current?.open();
  });

  const handleRefresh = useEventCallback(async () => {
    await generate(definitionId, report?.configuration);
    await load();
  });

  let title: string;
  let value: string;
  let showActions: boolean;

  if (definition === undefined) {
    title = intl.formatMessage({
      id: 'EAS_DASHBOARD_CARD_UNIVERSAL_MSG_ERROR',
      defaultMessage: 'Chyba reportu',
    });
    value = '';
    showActions = false;
  } else if (loading) {
    title = intl.formatMessage({
      id: 'EAS_DASHBOARD_CARD_UNIVERSAL_MSG_LOADING',
      defaultMessage: 'Načtení údajů',
    });
    value = '';
    showActions = false;
  } else if (report === undefined) {
    title = definition.groupLabel;
    value = definition.label;
    showActions = false;
  } else if (report?.columns.length !== 2) {
    title = intl.formatMessage({
      id: 'EAS_DASHBOARD_CARD_UNIVERSAL_MSG_UNSUPPORTED',
      defaultMessage: 'Nepodporovaný report',
    });
    value = '';
    showActions = false;
  } else {
    title = definition.groupLabel;
    value = definition.label;
    showActions = true;
  }

  return (
    <>
      <Card
        variant="outlined"
        className={clsx(
          classes.card,
          classes.cardUniversal,
          classesOverride?.card
        )}
      >
        <Box className={classes.cardRemove}>
          <IconButton
            size="small"
            className={classes.cardRemoveButton}
            aria-label={intl.formatMessage({
              id: 'EAS_DASHBOARD_CARD_UNIVERSAL_REMOVE_BTN_ARIA',
              defaultMessage: 'Odstranit',
            })}
            onClick={handleRemove}
          >
            <CloseIcon fontSize={'small'} />
          </IconButton>
        </Box>
        <Box className={classes.cardUniversalMenu}>
          {(definition?.inputFields?.length ?? 0) > 0 && (
            <IconButton
              aria-label={intl.formatMessage({
                id: 'EAS_DASHBOARD_CARD_UNIVERSAL_SETTINGS_BTN_ARIA',
                defaultMessage: 'Nastavení',
              })}
              onClick={handleOpenSettingsDialog}
            >
              <SettingsIcon />
            </IconButton>
          )}
          {definition?.autogenerate === false && (
            <IconButton
              aria-label={intl.formatMessage({
                id: 'EAS_DASHBOARD_CARD_UNIVERSAL_REFRESH_BTN_ARIA',
                defaultMessage: 'Obnova',
              })}
              onClick={handleRefresh}
            >
              <RefreshIcon />
            </IconButton>
          )}
        </Box>
        <CardContent className={classes.cardUniversalContent}>
          <Typography
            className={clsx(
              classes.cardUniversalTitle,
              classesOverride?.cardUniversalTitle
            )}
          >
            {title}
          </Typography>
          <Typography
            className={clsx(
              classes.cardUniversalValue,
              classesOverride?.cardUniversalValue
            )}
          >
            {value}
          </Typography>
        </CardContent>
      </Card>

      {report !== undefined && definition !== undefined && (
        <CardSettingsDialog
          ref={settingsDialogRef}
          report={report}
          definition={definition}
          load={load}
        />
      )}

      <Card className={classes.cardActions} variant="outlined">
        {showActions &&
          report?.data.map((item: Record<string, string>, i: number) => (
            <CardAction
              key={i}
              title={get(item, report.columns[0].datakey)}
              value={get(item, report.columns[1].datakey)}
            />
          ))}
      </Card>
    </>
  );
}
