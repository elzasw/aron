import { LocaleName } from '@eas/common-web';
import { getCookieValue } from '../../utils/cookies';

export const getLocale = (localeString?: string) => {
    if(!localeString) { return undefined; }
    const locale = Object.values(LocaleName).find((locale) => locale === localeString)
    if(locale){ return locale; }
    return undefined;
}

export const getLocaleFromCookie = (cookieName?: string) => {
  if(!cookieName){return undefined;}
  return getLocale(getCookieValue(cookieName));
}
