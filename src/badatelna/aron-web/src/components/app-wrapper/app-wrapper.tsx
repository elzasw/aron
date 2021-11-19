import React from 'react';
import classNames from 'classnames';
import { Props } from './types';
import { AppHeader } from '..';
import { useStyles } from './styles';
import { iOS } from '../../common-utils';
import { useConfiguration } from '../../components';

export function AppWrapper({ children, ...props }: Props) {
  const classes = useStyles();
  const configuration = useConfiguration();

  const isIOS = iOS();

  return (
    <div
      className={classNames(classes.appWrapper, isIOS && classes.iOSWrapper)}
    >
      {configuration.showHeader && <AppHeader {...props} />}
      {children}
    </div>
  );
}
