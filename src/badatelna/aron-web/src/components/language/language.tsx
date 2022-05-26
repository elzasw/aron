import React, { useContext } from 'react';
import Flags from 'country-flag-icons/react/3x2';
import classNames from 'classnames';

import { LocaleContext, LocaleName } from '@eas/common-web';

import { useStyles } from './styles';
import { useLayoutStyles } from '../../styles';
import { Props } from './types';

interface LocaleDef {
    name: LocaleName,
    Flag: any,
}
const locales:Record<string, LocaleDef> = {
  'en_US':{
    name: LocaleName.en,
    Flag: Flags.GB,
  },
  'cs_CZ':{
    name: LocaleName.cs,
    Flag: Flags.CZ,
  },
  'de_DE':{
    name: LocaleName.de,
    Flag: Flags.DE,
  },
  'fr_FR':{
    name: LocaleName.fr,
    Flag: Flags.FR,
  }
}

export function Language({ 
    className, 
    localizations = ['cs_CZ', 'en_US'],
}: Props) {
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();

  const { locale, switchLocale } = useContext(LocaleContext);
  const availableLocales:LocaleDef[] = [];

  localizations?.forEach((localization)=>{
    const locale = locales[localization];
    if(locale){availableLocales.push(locale)}
  })

  availableLocales.sort((a, b)=>{
    if(locale.name === a.name){return -1}
    if(locale.name === b.name){return 1}
    if(a.name < b.name){return -1}
    if(b.name < a.name){return 1}
    return 0;
  })
  
  const CurrentFlag = availableLocales.find(({name})=>name === locale.name)?.Flag;

  return (
    <div className={classNames(layoutClasses.flexCentered, className)}>
      <div className={classes.languageContainer}>
        {CurrentFlag && <CurrentFlag className={classes.flag}/>}
        <div className={classes.languageSelector}>
          {availableLocales.map(({name, Flag}, index)=>{
            return <Flag 
              key={index}
              className={classes.flag} 
              onClick={()=>{switchLocale(name)}}
            />
          })}
        </div>
      </div>
    </div>
  );
}
