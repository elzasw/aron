import React, { useState, useContext } from 'react';
import classNames from 'classnames';
import SearchIcon from '@material-ui/icons/Search';
import { useIntl } from 'react-intl';

import { TextField } from '../text-field';

import { Props } from './types';
import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import { Button } from '../button';
import { ModulePath, Message } from '../../enums';
import { NavigationContext } from '@eas/common-web';

export function Search({ main, onSearch, value = '' }: Props) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const { formatMessage } = useIntl();

  const [searchValue, setSearchValue] = useState<string>(value);

  const { navigate } = useContext(NavigationContext);

  const handleInputChange = (
    value: string | React.ChangeEvent<HTMLTextAreaElement | HTMLInputElement>
  ) => typeof value === 'string' && setSearchValue(value);

  const handleSearch = () =>
    (!main || searchValue) && onSearch({ query: searchValue });

  return (
    <div className={classes.search}>
      <div
        className={classNames(
          classes.searchInner,
          classes.searchBigInner,
          layoutClasses.flexCentered
        )}
      >
        <TextField
          className={classes.searchTextField}
          value={searchValue}
          onChange={handleInputChange}
          placeholder={formatMessage({
            id: Message.SEARCH,
          })}
          InputProps={{
            startAdornment: (
              <SearchIcon className={classes.searchIcon} color="disabled" />
            ),
          }}
          onKeyPress={(e) => {
            if (e.key === 'Enter') {
              handleSearch();
            }
          }}
        />
        <Button
          className={classes.searchButton}
          label={formatMessage({ id: Message.SEARCH_BTN })}
          color="primary"
          contained={true}
          onClick={handleSearch}
        />
      </div>
      {main ? (
        <div
          className={classNames(
            layoutClasses.flexEnd,
            spacingClasses.paddingTop
          )}
        >
          <div
            onClick={() => navigate(ModulePath.ARCH_DESC)}
            className={classes.searchAdvanced}
          >
            {formatMessage({ id: Message.ADVANCED_SEARCH })}
          </div>
        </div>
      ) : (
        <></>
      )}
    </div>
  );
}
