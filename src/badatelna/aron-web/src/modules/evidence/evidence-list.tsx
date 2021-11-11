import React, { useContext, useState, useEffect } from 'react';
import classNames from 'classnames';
import Pagination from '@material-ui/lab/Pagination';
import { FormattedMessage } from 'react-intl';
import { get } from 'lodash';

import { NavigationContext } from '@eas/common-web';

import { useStyles } from './styles';
import { useLayoutStyles } from '../../styles/layout';
import { useSpacingStyles } from '../../styles';
import { ModulePath, pageSizeOptions, Message, ApiUrl } from '../../enums';
import { Select, Loading } from '../../components';
import { useWindowSize, useApiList } from '../../common-utils';
import { useFilters } from './evidence-filters';
import { ApuEntity } from '../../types';

function ItemsList() {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const {
    page,
    pageSize,
    apiFilters,
    updatePage,
    updatePageSize,
  } = useFilters();

  const [result, loading] = useApiList(ApiUrl.APU, {
    json: {
      filters: apiFilters,
      sort: [
        {
          field: 'name',
          type: 'FIELD',
          order: 'ASC',
          sortMode: 'MIN',
        },
      ],
      offset: (page - 1) * pageSize,
      size: pageSize,
    },
  });

  const [infoEnabled, setInfoEnabled] = useState(false);

  const { width } = useWindowSize();

  const { navigate } = useContext(NavigationContext);

  useEffect(() => {
    if (loading) {
      setInfoEnabled(true);
    }
  }, [loading]);

  const items: ApuEntity[] = get(result, 'items', []);
  const count = get(result, 'count');

  return (
    <div className={classes.list}>
      {loading || items.length ? (
        <>
          <Loading {...{ loading }} />
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
                  spacingClasses.marginBottomSmall,
                  !description && classes.listItemTitleOnly
                )}
              >
                <h4>{name || 'Neznámé'}</h4>
                {description && <p>{description}</p>}
              </div>
            ))}
            {items.length ? (
              <div
                className={classNames(
                  layoutClasses.flexSpaceBetween,
                  layoutClasses.flexColumnPhone,
                  spacingClasses.paddingTopSmall
                )}
              >
                <Pagination
                  count={
                    count ? Math.ceil(Math.min(count, 9999) / pageSize) : 0
                  }
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
            ) : (
              <></>
            )}
          </div>
        </>
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

export function EvidenceList() {
  const { initialized, loading } = useFilters();
  const classes = useStyles();

  return (
    <div className={classes.listContainer}>
      {initialized ? (
        <ItemsList />
      ) : (
        <Loading {...{ loading: !initialized || loading }} />
      )}
    </div>
  );
}
