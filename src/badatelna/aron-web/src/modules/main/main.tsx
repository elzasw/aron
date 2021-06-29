import React from 'react';
import classNames from 'classnames';

import { Body } from './body';
import { Footer } from './footer';
import { useStyles } from './styles';
import { useLayoutStyles } from '../../styles';
import { Icon } from '../../components';
import { Props } from './types';

export const Main = (props: Props) => {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();

  return (
    <div
      className={classNames(classes.main, layoutClasses.flexColumnSpaceBetween)}
    >
      <Icon className={classes.mainBackgroundIcon} />
      <Body />
      <Footer {...props} />
    </div>
  );
};
