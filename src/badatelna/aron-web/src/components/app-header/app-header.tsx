import React, { useState, useContext, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import classNames from 'classnames';
import IconButton from '@material-ui/core/IconButton';
import MenuIcon from '@material-ui/icons/Menu';

import { LocaleContext, LocaleName } from '@eas/common-web';

import { getAppHeaderItems, ApiUrl } from '../../enums';
import { AppTitle } from '../app-title';
import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import { Language } from '../language';
import { openUrl, useGet } from '../../common-utils';
import { Props } from './types';
import { useConfiguration } from '../configuration'

export function AppHeader({ pageTemplate, ...props }: Props) {
  const configuration = useConfiguration();
  const classes = useStyles({compactAppHeader: configuration.compactAppHeader});
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();
  const location = useLocation();
  const [open, setOpen] = useState(false);

  const [result] = useGet<string>(ApiUrl.HELP, { textResponse: true });

  const { switchLocale } = useContext(LocaleContext);

  const localizations = pageTemplate?.localizations;

  useEffect(() => {
    if (
      localizations &&
      localizations.length === 1 &&
      typeof localizations[0] === 'string'
    ) {
      switchLocale(
        localizations[0] === 'cs_CZ' ? LocaleName.cs : LocaleName.en
      );
    }
  }, [localizations, switchLocale]);

  const handleHelp = () =>
    result && typeof result === 'string' && openUrl(result);

  const showLanguages = localizations && localizations.length > 1 && !configuration.localeCookieName;

  return (
    <div className={classes.appHeader}>
      <div
        className={classNames(
          classes.appHeaderInner,
          layoutClasses.flexSpaceBetween
        )}
      >
        <AppTitle {...props} />
        <div className={classes.appHeaderItems}>
          {getAppHeaderItems(configuration).map(({ path, label }, i) =>
            !path ? (
              <div
                key={i}
                onClick={handleHelp}
                className={classNames(
                  classes.appHeaderItem,
                  layoutClasses.flexCentered,
                  spacingClasses.paddingHorizontal
                )}
              >
                {label}
              </div>
            ) : (
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
            )
          )}
          {showLanguages && 
             <Language 
               localizations={pageTemplate?.localizations} 
               className={spacingClasses.marginLeft} 
             />}
        </div>
        <>
          <div className={classNames(classes.mobileMenu, layoutClasses.flex)}>
            <IconButton
              className={classes.toggleMenuButton}
              onClick={() => setOpen((prev) => !prev)}
            >
              <MenuIcon />
            </IconButton>
            {showLanguages && <Language className={spacingClasses.marginLeft} />}
          </div>
          {open ? (
            <div
              className={classes.appHeaderItemsMobile}
              onClick={() => setOpen(false)}
            >
              {getAppHeaderItems(configuration).map(({ path, label }, i) =>
                !path ? (
                  <div
                    key={i}
                    onClick={handleHelp}
                    className={classes.appHeaderItemMobile}
                  >
                    {label}
                  </div>
                ) : (
                  <Link
                    key={path}
                    to={{
                      pathname: path,
                    }}
                    className={classes.appHeaderItemMobile}
                  >
                    {label}
                  </Link>
                )
              )}
            </div>
          ) : (
            <></>
          )}
        </>
      </div>
    </div>
  );
}
