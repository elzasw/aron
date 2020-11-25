import React, { useContext, useState, ChangeEvent } from 'react';
import { useIntl } from 'react-intl';
import MuiTextField from '@material-ui/core/TextField';
import MuiInputAdornment from '@material-ui/core/InputAdornment/InputAdornment';
import MuiSearchIcon from '@material-ui/icons/Search';
import MuiClearIcon from '@material-ui/icons/Clear';
import { useStyles } from './table-styles';
import { TableContext } from './table-context';
import { useEventCallback } from 'utils/event-callback-hook';

export function TableSearchbar() {
  const classes = useStyles();

  const { disabled, setSearchQuery } = useContext(TableContext);

  const intl = useIntl();

  const [searchText, setSearchTextChange] = useState('');

  const handleSearchTextChange = useEventCallback(
    (e: ChangeEvent<HTMLInputElement>) => {
      const value = e.target.value;

      setSearchTextChange(value);
      setSearchQuery(value);
    }
  );

  const handleClear = useEventCallback(() => {
    setSearchTextChange('');
    setSearchQuery('');
  });

  return (
    <MuiTextField
      disabled={disabled}
      classes={{ root: classes.searchTextField }}
      autoComplete="off"
      id="outlined-search"
      type="search"
      margin="dense"
      placeholder={intl.formatMessage({
        id: 'EAS_TABLE_SEARCHBAR_BTN_SEARCH',
        defaultMessage: 'Vyhledat',
      })}
      variant="outlined"
      value={searchText}
      onChange={handleSearchTextChange}
      InputProps={{
        classes: { root: classes.searchTextFieldInput },
        endAdornment: (
          <MuiInputAdornment position="end">
            {searchText && (
              <MuiClearIcon
                classes={{ root: classes.clearIcon }}
                onClick={handleClear}
              />
            )}

            <MuiSearchIcon classes={{ root: classes.searchIcon }} />
          </MuiInputAdornment>
        ),
        style: {
          boxShadow: 'none',
          paddingRight: 0,
        },
      }}
    />
  );
}
