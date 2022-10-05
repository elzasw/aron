import React, { useContext } from 'react';
import { useLocation } from 'react-router-dom';
import classNames from 'classnames';
import { FormattedMessage } from 'react-intl';

import { NavigationContext } from '@eas/common-web';

import { ModulePath, Message } from '../../enums';
import { useStyles } from './styles';
import { Props } from './types';
import { useConfiguration } from '../configuration'

export function AppTitle({ appLogo, appTopImage }: Props) {
  const {
    compactAppHeader, 
    showAppTopImage, 
    showAppLogo
  } = useConfiguration();
  const classes = useStyles({compactAppHeader});
  const location = useLocation();

  const { navigate } = useContext(NavigationContext);

  const isClickable = location.pathname !== ModulePath.MAIN;

  return (
    <div
      className={classNames(
        classes.appTitle,
        isClickable && !appLogo && classes.appTitleClickable
      )}
      onClick={() => isClickable && !appLogo && navigate(ModulePath.MAIN)}
    >
      {showAppLogo &&
        (appLogo ? (
          <img
            src={appLogo}
            className={classNames(
              classes.appTitleLogo,
              isClickable && classes.appTitleLogoClickable
            )}
            onClick={() => isClickable && navigate(ModulePath.MAIN)}
          />
        ) : (
          <>
            <span className={classes.appTitleFirst}>
              <FormattedMessage id={Message.ARCHIVE} />
            </span>
            &nbsp;
            <FormattedMessage id={Message.ONLINE} />
          </>
      ))}
      {showAppTopImage && !compactAppHeader && 
        <img className={classes.appTitleTopImage} src={appTopImage}/>
      }
      <div />
    </div>
  );
}
