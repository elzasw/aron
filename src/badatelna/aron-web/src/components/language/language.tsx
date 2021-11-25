import React, { useContext } from 'react';
import Flags from 'country-flag-icons/react/3x2';
import classNames from 'classnames';

import { LocaleContext, LocaleName } from '@eas/common-web';

import { useStyles } from './styles';
import { useLayoutStyles } from '../../styles';
import { Props } from './types';

const locales = [
  {
    name: LocaleName.en,
    Flag: Flags.GB,
  },
  {
    name: LocaleName.cs,
    Flag: Flags.CZ,
  },
  // {
  //   name: LocaleName.de,
  //   Flag: Flags.DE,
  // }
]

export function Language({ className }: Props) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();

  const { locale, switchLocale } = useContext(LocaleContext);

  locales.sort((a, b)=>{
    if(locale.name === a.name){return -1}
    if(locale.name === b.name){return 1}
    if(a.name < b.name){return -1}
    if(b.name < a.name){return 1}
    return 0;
  })
  
  const CurrentFlag = locales.find(({name})=>name === locale.name)?.Flag;

  return (
    <div className={classNames(layoutClasses.flexCentered, className)}>
      <div className={classes.languageContainer}>
        <CurrentFlag className={classes.flag}/>
        <div className={classes.languageSelector}>
          {locales.map(({name, Flag})=>{
            return <Flag 
              className={classes.flag} 
              onClick={()=>{switchLocale(name)}}
            />
          })}
        </div>
      </div>
    </div>
  );
}
