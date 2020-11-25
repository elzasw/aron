import React, { useContext } from 'react';
import { useLocation } from 'react-router-dom';
import classNames from 'classnames';

import { NavigationContext } from '@eas/common-web';

import { ModulePath } from '../../enums';
import { Icon, IconType } from '..';
import { useStyles } from './styles';
import { useLayoutStyles } from '../../styles';

export function AppTitle() {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const location = useLocation();

  const { navigate } = useContext(NavigationContext);

  const isClickable = location.pathname !== ModulePath.MAIN;

  return (
    <div
      className={classNames(
        classes.appTitle,
        layoutClasses.flexCentered,
        isClickable && classes.appTitleClickable
      )}
      onClick={() => isClickable && navigate(ModulePath.MAIN)}
    >
      <Icon
        className={classes.invertColor}
        type={IconType.BOOK}
        size={42}
        color="#fff"
      />
      &nbsp;&nbsp;&nbsp;
      <span className={classes.appTitleFirst}>Archiv</span>
      &nbsp;Online
    </div>
  );
}
