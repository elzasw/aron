import React, { useContext, useState } from 'react';
import classNames from 'classnames';
import { FormattedMessage } from 'react-intl';
import CircularProgress from '@material-ui/core/CircularProgress';
import { NavigationContext } from '@eas/common-web';

import { Icon, Search } from '../../components';
import { ModulePath, Message, ApiUrl } from '../../enums';
import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import { getPathByType, getUrlWithQuery, useGet } from '../../common-utils';
import { searchOptions } from '../../enums';
import { ClickableSelection } from '../../components/clickable-selection/';
import { FavouriteQuery } from '../../types';

export const Body: React.FC = () => {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const { navigate } = useContext(NavigationContext);

  const [selectedOptions, setSelectedOptions] = useState<any[]>([]);

  const [favouriteQueries, loadingFavouriteQueries] = useGet<FavouriteQuery[]>(
    ApiUrl.FAVORITE_QUERY
  );

  return (
    <div
      className={classNames(
        classes.mainBody,
        layoutClasses.flexCentered,
        spacingClasses.paddingVerticalBig
      )}
    >
      <div className={classes.mainBodyInner}>
        <h1>
          <FormattedMessage id={Message.ENTER_SEARCH_QUERY} />
        </h1>
        <Search
          main={true}
          onSearch={({ query }) =>
            navigate(
              getUrlWithQuery(
                selectedOptions[0]?.path || ModulePath.APU,
                query,
                selectedOptions[0]?.filters || []
              )
            )
          }
        />
        <ClickableSelection
          radio={true}
          options={searchOptions}
          onChange={setSelectedOptions}
        />
        {(favouriteQueries || loadingFavouriteQueries) && (
          <>
            <h4 className={spacingClasses.marginTopBig}>
              <FormattedMessage id={Message.FAVOURITE_QUERIES} />
            </h4>
            <div
              className={classNames(
                classes.mainFavourite,
                layoutClasses.flex,
                layoutClasses.flexWrap,
                spacingClasses.paddingBottomBig
              )}
            >
              {loadingFavouriteQueries ? (
                <CircularProgress />
              ) : (
                (favouriteQueries || []).map(
                  ({ icon, label, type, query, filters }) => (
                    <div
                      key={label}
                      onClick={() =>
                        navigate(
                          getUrlWithQuery(
                            type ? getPathByType(type) : ModulePath.APU,
                            query,
                            filters
                          )
                        )
                      }
                      className={classNames(
                        layoutClasses.flexAlignCenter,
                        spacingClasses.marginBottomSmall
                      )}
                    >
                      <Icon type={icon} />
                      &nbsp;&nbsp;&nbsp;{label}
                    </div>
                  )
                )
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
};
