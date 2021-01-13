import React, { useContext } from 'react';
import Flags from 'country-flag-icons/react/3x2';
import classNames from 'classnames';

import { LocaleContext, LocaleName } from '@eas/common-web';

import { useStyles } from './styles';
import { useLayoutStyles } from '../../styles';
import { Props } from './types';

const switchFn = (locale: { name: string }, cs: any, en: any) => {
  switch (locale.name) {
    case LocaleName.en:
      return en;
    default:
      return cs;
  }
};

export function Language({ className }: Props) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();

  const { locale, switchLocale } = useContext(LocaleContext);

  const Component = switchFn(locale, Flags.GB, Flags.CZ);

  return (
    <div className={classNames(layoutClasses.flexCentered, className)}>
      <Component
        className={classes.flag}
        onClick={() =>
          switchLocale(switchFn(locale, LocaleName.en, LocaleName.cs))
        }
      />
    </div>
  );
}
