import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import classNames from 'classnames';
import IconButton from '@material-ui/core/IconButton';
import MenuIcon from '@material-ui/icons/Menu';

import { appHeaderItems } from '../../enums';
import { AppTitle } from '../app-title';
import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';

export function AppHeader() {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();
  const location = useLocation();
  const [open, setOpen] = useState(false);

  return (
    <div className={classes.appHeader}>
      <div
        className={classNames(
          classes.appHeaderInner,
          layoutClasses.flexSpaceBetween
        )}
      >
        <AppTitle />
        <div className={classes.appHeaderItems}>
          {appHeaderItems.map(({ path, label }) => (
            <Link
              key={path}
              to={{
                pathname: path,
              }}
              className={classNames(
                classes.appHeaderItem,
                layoutClasses.flexCentered,
                spacingClasses.paddingHorizontal,
                location.pathname === path && classes.appHeaderItemActive
              )}
            >
              {label}
            </Link>
          ))}
        </div>
        <>
          <IconButton
            className={classes.toggleMenuButton}
            onClick={() => setOpen((prev) => !prev)}
          >
            <MenuIcon />
          </IconButton>
          {open ? (
            <div
              className={classes.appHeaderItemsMobile}
              onClick={() => setOpen(false)}
            >
              {appHeaderItems.map(({ path, label }) => (
                <Link
                  key={path}
                  to={{
                    pathname: path,
                  }}
                  className={classes.appHeaderItemMobile}
                >
                  {label}
                </Link>
              ))}
            </div>
          ) : (
            <></>
          )}
        </>
      </div>
    </div>
  );
}
