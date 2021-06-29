import React, { useContext } from 'react';
import clsx from 'clsx';
import { useIntl, FormattedMessage } from 'react-intl';
import Typography from '@material-ui/core/Typography';
import PrintIcon from '@material-ui/icons/Print';
import CachedIcon from '@material-ui/icons/Cached';
import ViewColumnIcon from '@material-ui/icons/ViewColumn';
import SortIcon from '@material-ui/icons/Sort';
import FilterListIcon from '@material-ui/icons/FilterList';
import { useStyles } from './table-styles';
import { TableSelectedContext, TableContext } from './table-context';
import { TableToolbarButton } from './table-toolbar-button';
import { BulkActionButton } from './bulk-action-menu/bulk-action-button';
import { TableToolbarProps } from './table-types';
import { NamedSettingsButton } from './saved/named-settings-button';

export function TableToolbar({ before, after }: TableToolbarProps) {
  const classes = useStyles();

  const {
    totalCount,
    loadedCount,
    tableName,
    filtersState,
    showRefreshButton,
    showColumnButton,
    showResetSortsButton,
    showFilterButton,
    showNamedSettingsButton,
    showReportButton,
    showBulkActionButton,
    disabledRefreshButton,
    disabledColumnButton,
    disabledResetSortsButton,
    disabledFilterButton,
    disabledNamedSettingsButton,
    disabledBulkActionButton,
    disabledReportButton,
    reportTag,
    bulkActions,
    refresh,
    openColumnDialog,
    openExportDialog,
    openFilterDialog,
    resetSorts,
  } = useContext(TableContext);
  const { selected } = useContext(TableSelectedContext);
  const selectedCount = selected.length;

  const intl = useIntl();

  const filterCount = filtersState.filter((filter) => filter.enabled).length;

  return (
    <>
      <div
        className={clsx(classes.toolbarWrapper, {
          [classes.toolbarSelected]: selectedCount > 0,
        })}
      >
        <Typography className={classes.toolbarText} component="span">
          {selectedCount > 0 ? (
            <span className={classes.toolbarSelectedLabel}>
              <FormattedMessage
                id="EAS_TABLE_TOOLBAR_SELECTED"
                defaultMessage={`{selectedCount, plural, 
                  one {1 vybraná položka}
                  few {# vybrané položky}
                  other {# vybraných položek}}`}
                values={{ selectedCount }}
              />
            </span>
          ) : (
            <>
              {tableName ?? ''}
              {` (${loadedCount}/${Math.max(totalCount, 0)})`}
            </>
          )}
        </Typography>
        <ul className={classes.toolbarButtonList}>
          {before}
          {showRefreshButton && (
            <TableToolbarButton
              label={<CachedIcon />}
              disabled={disabledRefreshButton}
              onClick={refresh}
              tooltip={intl.formatMessage({
                id: 'EAS_TABLE_TOOLBAR_BTN_REFRESH',
                defaultMessage: 'Obnovit data',
              })}
            />
          )}
          {showResetSortsButton && (
            <TableToolbarButton
              label={<SortIcon />}
              disabled={disabledResetSortsButton}
              onClick={resetSorts}
              tooltip={intl.formatMessage({
                id: 'EAS_TABLE_TOOLBAR_BTN_RESET_SORTS',
                defaultMessage: 'Obnovení výchozího řazení',
              })}
            />
          )}
          {showColumnButton && (
            <TableToolbarButton
              label={<ViewColumnIcon />}
              disabled={disabledColumnButton}
              onClick={openColumnDialog}
              tooltip={intl.formatMessage({
                id: 'EAS_TABLE_TOOLBAR_BTN_COLUMN_DIALOG_OPEN',
                defaultMessage: 'Nastavení sloupců',
              })}
            />
          )}
          {showFilterButton && (
            <TableToolbarButton
              label={<FilterListIcon />}
              disabled={disabledFilterButton}
              onClick={openFilterDialog}
              tooltip={intl.formatMessage({
                id: 'EAS_TABLE_TOOLBAR_BTN_FILTER_DIALOG_OPEN',
                defaultMessage: 'Nastavení filtrů',
              })}
              endIcon={
                <Typography
                  className={clsx(classes.toolbarText, classes.toolbarTextSub, {
                    'Mui-disabled': disabledFilterButton,
                  })}
                  component="span"
                >
                  {filterCount}
                </Typography>
              }
            />
          )}
          {showNamedSettingsButton && reportTag != null && (
            <NamedSettingsButton
              disabled={disabledNamedSettingsButton}
              tag={reportTag}
            />
          )}
          {showBulkActionButton && (
            <BulkActionButton
              disabled={disabledBulkActionButton || bulkActions.length === 0}
              actions={bulkActions}
            />
          )}
          {showReportButton && (
            <TableToolbarButton
              label={<PrintIcon />}
              disabled={disabledReportButton}
              onClick={openExportDialog}
              tooltip={intl.formatMessage({
                id: 'EAS_TABLE_TOOLBAR_BTN_EXPORT_DIALOG_OPEN',
                defaultMessage: 'Tisk',
              })}
            />
          )}
          {after}
        </ul>
      </div>
    </>
  );
}
