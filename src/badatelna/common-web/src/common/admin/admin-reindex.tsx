import React, { useEffect, useState, useMemo, useContext } from 'react';
import { noop } from 'lodash';
import Grid from '@material-ui/core/Grid';
import Box from '@material-ui/core/Box';
import Button from '@material-ui/core/Button';
import CircularProgress from '@material-ui/core/CircularProgress';
import { abortableFetch } from 'utils/abortable-fetch';
import { TableField } from 'components/table-field/table-field';
import {
  TableFieldColumn,
  TableFieldCellProps,
} from 'components/table-field/table-field-types';
import { useEventCallback } from 'utils/event-callback-hook';
import { Checkbox } from 'components/checkbox/checkbox';
import { FormattedMessage, useIntl } from 'react-intl';
import { SnackbarContext } from 'composite/snackbar/snackbar-context';
import { SnackbarVariant } from 'composite/snackbar/snackbar-types';
import { AdminContext } from './admin-context';

/**
 * Gets all stores.
 */
export function getRepositories(url: string) {
  return abortableFetch(url, {
    headers: new Headers({
      'Content-Type': 'application/json',
    }),
    method: 'GET',
  });
}

/**
 * Gets all stores.
 */
export function reindexCall(url: string, names: string[]) {
  return abortableFetch(url, {
    headers: new Headers({
      'Content-Type': 'application/json',
    }),
    method: 'POST',
    body: JSON.stringify(names),
  });
}

interface RepositoryState {
  name: string;
  active: boolean;
}

export function AdminReindex() {
  const intl = useIntl();
  const { showSnackbar } = useContext(SnackbarContext);
  const { reindexUrl } = useContext(AdminContext);
  const [repositories, setRepositiories] = useState<RepositoryState[]>([]);
  const [loading, setLoading] = useState(false);

  const CheckboxCell = useMemo(
    () =>
      function CheckboxCell<OBJECT>({
        value,
        index,
      }: TableFieldCellProps<OBJECT>) {
        return (
          <Checkbox
            value={value}
            onChange={() => {
              setRepositiories((repositories) => [
                ...repositories.slice(0, index),
                { ...repositories[index], active: !repositories[index].active },
                ...repositories.slice(index + 1),
              ]);
            }}
          />
        );
      },
    []
  );

  const columns: TableFieldColumn<RepositoryState>[] = [
    {
      name: 'Výběr',
      datakey: 'active',
      width: 100,
      CellComponent: CheckboxCell,
    },
    {
      name: 'Repository',
      datakey: 'name',
      width: 500,
    },
  ];

  const reindex = useEventCallback(async (names: string[]) => {
    try {
      setLoading(true);

      await reindexCall(reindexUrl, names).none();

      const message = intl.formatMessage({
        id: 'EAS_DEVTOOLS_REINDEX_MSG_SUCCESS',
        defaultMessage: 'Reindexace dokončena',
      });

      showSnackbar(message, SnackbarVariant.SUCCESS);

      setLoading(false);
    } catch (err) {
      setLoading(false);

      if (err.name !== 'AbortError') {
        const message = intl.formatMessage(
          {
            id: 'EAS_DEVTOOLS_REINDEX_MSG_ERROR',
            defaultMessage: 'Chyba reindexace: {detail}',
          },
          { detail: err.message }
        );

        showSnackbar(message, SnackbarVariant.ERROR);

        throw err;
      }
    }
  });

  const reindexSelected = useEventCallback(() => {
    const names = repositories
      .filter((repository) => repository.active)
      .map((repository) => repository.name);
    return reindex(names);
  });

  const reindexAll = useEventCallback(() => {
    const names = repositories.map((repository) => repository.name);
    return reindex(names);
  });

  useEffect(() => {
    getRepositories(reindexUrl)
      .json()
      .then((repositiories: string[]) => {
        setRepositiories(
          repositiories.map((repository) => ({
            name: repository,
            active: false,
          }))
        );
      });
  }, [reindexUrl]);

  return (
    <Grid container>
      <Grid item xs={false} sm={1} />
      <Grid item xs={12} sm={10}>
        <Box height={20} />
        <TableField
          maxRows={15}
          showToolbar={false}
          showRadioCond={() => false}
          value={repositories}
          columns={columns}
          onChange={noop}
        />
        <Box height={20} />
      </Grid>
      <Grid item xs={false} sm={1} />

      <Grid item xs={false} sm={1} />
      <Grid item xs={12} sm={10}>
        <Box display="flex" flexDirection="row-reverse">
          <Button
            variant="contained"
            color="primary"
            type="submit"
            disabled={loading}
            startIcon={
              loading && <CircularProgress size="20px" color="inherit" />
            }
            onClick={reindexAll}
          >
            <FormattedMessage
              id="EAS_DEVTOOLS_REINDEX_BTN_REINDEX_ALL"
              defaultMessage="Reindexovat vše"
            />
          </Button>
          <Box width={10} />
          <Button
            variant="contained"
            color="primary"
            type="submit"
            disabled={loading}
            startIcon={
              loading && <CircularProgress size="20px" color="inherit" />
            }
            onClick={reindexSelected}
          >
            <FormattedMessage
              id="EAS_DEVTOOLS_REINDEX_BTN_REINDEX"
              defaultMessage="Reindexovat"
            />
          </Button>
        </Box>
      </Grid>
      <Grid item xs={false} sm={1} />
    </Grid>
  );
}
