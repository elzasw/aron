import React, { useContext, useState, useEffect } from 'react';
import classNames from 'classnames';
import LinearProgress from '@material-ui/core/LinearProgress';
import Pagination from '@material-ui/lab/Pagination';
import { FormattedMessage } from 'react-intl';

import { NavigationContext } from '@eas/common-web';

import { ListProps } from './types';
import { useStyles } from './styles';
import { useLayoutStyles } from '../../styles/layout';
import { useSpacingStyles } from '../../styles';
import { ModulePath, pageSizeOptions, Message } from '../../enums';
import { Select } from '../../components';
import { useWindowSize } from '../../common-utils';

export function EvidenceList({
  loading,
  items,
  count,
  page,
  pageSize,
  updatePage,
  updatePageSize,
}: ListProps) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const [infoEnabled, setInfoEnabled] = useState(false);

  const { width } = useWindowSize();

  const { navigate } = useContext(NavigationContext);

  useEffect(() => {
    if (loading) {
      setInfoEnabled(true);
    }
  }, [loading]);

  return (
    <div className={classes.list}>
      {loading ? (
        <LinearProgress />
      ) : items.length ? (
        <div
          className={classNames(
            spacingClasses.padding,
            spacingClasses.paddingBottomSmall
          )}
        >
          {items.map(({ id, name, description }) => (
            <div
              key={id}
              onClick={() => navigate(`${ModulePath.APU}/${id}`)}
              className={classNames(
                classes.listItem,
                spacingClasses.marginBottom
              )}
            >
              <h4>{name || 'Neznámé'}</h4>
              <p>{description}</p>
            </div>
          ))}
          <div
            className={classNames(
              layoutClasses.flexSpaceBetween,
              layoutClasses.flexColumnPhone
            )}
          >
            <Pagination
              count={count}
              size={width > 600 ? 'medium' : 'small'}
              color="primary"
              page={page}
              onChange={(_, page) => updatePage(page)}
              className={spacingClasses.marginBottomSmall}
            />
            <div
              className={classNames(
                classes.listPageNumber,
                spacingClasses.marginBottomSmall
              )}
            >
              <span>
                <FormattedMessage id={Message.PER_PAGE} />
                :&nbsp;&nbsp;&nbsp;
              </span>
              <div className={classes.pageSizeSelect}>
                <Select
                  value={{ id: pageSize.toString(), name: pageSize }}
                  options={pageSizeOptions}
                  onChange={({ id }) => updatePageSize(id)}
                  clearable={false}
                />
              </div>
            </div>
          </div>
        </div>
      ) : infoEnabled ? (
        <div className={classNames(classes.listEmpty, spacingClasses.padding)}>
          <FormattedMessage id={Message.NO_ITEMS_FOUND} />
        </div>
      ) : (
        <></>
      )}
    </div>
  );
}
