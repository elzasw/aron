import React from 'react';
import { Link } from 'react-router-dom';
import classNames from 'classnames';

import { ModulePath } from '../../enums';
import { Props } from './types';
import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';

export function Module({ children, items }: Props) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const allItems = [{ path: ModulePath.MAIN, label: 'Úvod' }, ...items];

  return (
    <div className={classes.module}>
      <div
        className={classNames(
          classes.breadcrumbs,
          layoutClasses.flexAlignCenter,
          spacingClasses.paddingLeftBig
        )}
      >
        {allItems.map(({ path, label }, i) => (
          <div key={label} className={layoutClasses.flexCentered}>
            {i ? <div>&nbsp;&nbsp;&nbsp;/&nbsp;&nbsp;&nbsp;</div> : ''}
            {path && i < allItems.length - 1 ? (
              <Link to={{ pathname: path }} className={classes.breadcrumbsLink}>
                {label}
              </Link>
            ) : (
              label
            )}
          </div>
        ))}
      </div>
      {children}
    </div>
  );
}
