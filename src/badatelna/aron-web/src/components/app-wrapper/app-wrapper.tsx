import React from 'react';
import classNames from 'classnames';
import { Props } from './types';
import { AppHeader } from '..';
import { useStyles } from './styles';
import { iOS } from '../../common-utils';

export function AppWrapper({ children, ...props }: Props) {
  const classes = useStyles();

  const isIOS = iOS();

  return (
    <div
      className={classNames(classes.appWrapper, isIOS && classes.iOSWrapper)}
    >
      <AppHeader {...props} />
      {children}
    </div>
  );
}
