import React from 'react';
import { Icon, IconType } from '../../components';
import classNames from 'classnames';

import { Body } from './body';
import { Footer } from './footer';
import { useStyles } from './styles';
import { useLayoutStyles } from '../../styles';

export const Main: React.FC = () => {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();

  return (
    <div className={classes.main}>
      <Icon type={IconType.BOOK} className={classes.mainBackgroundIcon} />
      <div className={classNames(classes.mainInner, layoutClasses.flexColumn)}>
        <Body />
        <Footer />
      </div>
    </div>
  );
};
