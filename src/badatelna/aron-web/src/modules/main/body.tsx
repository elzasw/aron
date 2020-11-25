import React, { useContext } from 'react';
import classNames from 'classnames';

import { NavigationContext } from '@eas/common-web';

import { Icon, Search } from '../../components';
import { favouriteQueries, ModulePath } from '../../enums';
import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import { getUrlWithQuery } from '../../common-utils';

export const Body: React.FC = () => {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const { navigate } = useContext(NavigationContext);

  return (
    <div
      className={classNames(
        classes.mainBody,
        layoutClasses.flexCentered,
        spacingClasses.paddingVerticalBig
      )}
    >
      <div className={classes.mainBodyInner}>
        <h1>Zadejte hledaný dotaz</h1>
        <Search
          main={true}
          onSearch={({ query }) =>
            navigate(getUrlWithQuery(ModulePath.SEARCH, query))
          }
        />
        <h4 className={spacingClasses.marginTopBig}>Oblíbené dotazy</h4>
        <div
          className={classNames(
            classes.mainFavourite,
            layoutClasses.flex,
            layoutClasses.flexWrap,
            spacingClasses.paddingBottomBig
          )}
        >
          {favouriteQueries.map(({ icon, label }) => (
            <div
              key={label}
              className={classNames(
                layoutClasses.flexAlignCenter,
                spacingClasses.marginBottomSmall
              )}
            >
              <Icon type={icon} className="icon" />
              &nbsp;&nbsp;&nbsp;{label}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
