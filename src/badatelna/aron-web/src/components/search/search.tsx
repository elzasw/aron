import React, { useState, useContext, useEffect } from 'react';
import classNames from 'classnames';
import SearchIcon from '@material-ui/icons/Search';
import IconButton from '@material-ui/core/IconButton';
import { useIntl } from 'react-intl';

import { TextField } from '../text-field';

import { Props } from './types';
import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import { Button } from '../button';
import { ModulePath, Message } from '../../enums';
import { NavigationContext } from '@eas/common-web';
import { usePrevious } from '../../common-utils';

export function Search({
  main,
  onSearch,
  value,
  placeholder = Message.SEARCH,
}: Props) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const { formatMessage } = useIntl();

  const { navigate } = useContext(NavigationContext);

  const prevValue = usePrevious(value);

  const [searchValue, setSearchValue] = useState<string>(value || '');

  useEffect(() => {
    if (prevValue !== value && value !== searchValue) {
      setSearchValue(value);
    }
  }, [value, prevValue, searchValue]);

  const handleInputChange = (
    value: string | React.ChangeEvent<HTMLTextAreaElement | HTMLInputElement>
  ) => {
    if(value == ''){
      handleSearchReset(); 
      return;
    }
      typeof value === 'string' && setSearchValue(value)
  };

  const handleSearch = () => onSearch({ query: searchValue });
  const handleSearchReset = () => {
    onSearch({ query: '' });
    setSearchValue('');
  }

  return (
    <div className={classes.search}>
      <div
        className={classNames(
          classes.searchInner,
          classes.searchBigInner,
          layoutClasses.flexCentered
        )}
      >
        <div className={classNames(classes.searchTextFieldWrapper, layoutClasses.flexCentered)}>
          <TextField
            className={classNames(classes.searchTextField, searchValue && classes.searchTextInserted)}
            value={searchValue}
            onChange={handleInputChange}
            placeholder={formatMessage({
              id: placeholder,
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
          {(value || searchValue) && 
            <IconButton 
              aria-label='delete' 
              onClick={handleSearchReset}
              type='button'
              size='small'
              className={classes.searchResetButton}
            >
              <span className='fas fa-times'/>
            </IconButton>
          }
        </div>
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
