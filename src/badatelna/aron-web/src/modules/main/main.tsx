import React from 'react';
import classNames from 'classnames';

import { Body } from './body';
import { Footer } from './footer';
import { useStyles } from './styles';
import { useLayoutStyles } from '../../styles';
import { Icon } from '../../components';
import { IconType } from '../../enums';

export const Main: React.FC = () => {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();

  return (
    <div
      className={classNames(classes.main, layoutClasses.flexColumnSpaceBetween)}
    >
      <Icon type={IconType.BOOK} className={classes.mainBackgroundIcon} />
      <Body />
      <Footer />
    </div>
  );
};
