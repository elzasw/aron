import React, { useMemo, useEffect, useState } from 'react';
import { BrowserRouter, Switch, Route } from 'react-router-dom';
import { Helmet } from 'react-helmet';
import { FormattedMessage } from 'react-intl';

import {
  ThemeProvider,
  LocaleProvider,
  SnackbarProvider,
  NavigationProvider,
  LocaleName,
} from '@eas/common-web';

import { AppWrapper, Loading, ConfigurationProvider, useConfiguration, getLocaleFromCookie, ConfigurationType } from './components';
import { getNavigationItems, ApiUrl, messages, Message } from './enums';
import {
  useGet,
  AppStateProvider,
  parseYaml,
  getPageTemplateLogo,
  getPageTemplateTopImage,
} from './common-utils';
import { useAppStyles, colorWhite } from './styles';

const primary = {
  light: '#70a0c9',
  main: '#306ea4',
  dark: '#1a3a56',
};
const editing = colorWhite;
const highlight = '#feb28a';
const fontSize = 13;
const fontFamily = [
  'Proxima Nova',
  'Helvetica Neue',
  'Helvetica',
  'Arial',
  'sans-serif',
];

const overrideLocale = (messages: Record<string, Record<Message, string>>, configuration: ConfigurationType) => {
  const newMessages = {...messages};

  Object.entries(newMessages).forEach(([locale, messages])=>{
    newMessages[locale] = {
      ...messages,
      ...(configuration.localeOverride?.[locale] || {})
    }
  })

  return newMessages;
}

function AppComponent() {
  const classes = useAppStyles();

  const configuration = useConfiguration();
  const [loadingImages, setLoadingImages] = useState(true);
  const [appLogo, setAppLogo] = useState<string>();
  const [appTopImage, setAppTopImage] = useState<string>();

  const [apuPartTypes, loadingApuPartTypes] = useGet(ApiUrl.APU_PART_TYPE);
  const [apuPartItemTypes, loadingApuPartItemTypes] = useGet(
    ApiUrl.APU_PART_ITEM_TYPE
  );
  const [facets, loadingFacets] = useGet(ApiUrl.FACETS);
  const [pageTemplateString, loadingPageTemplate] = useGet<string>(
    ApiUrl.PAGE_TEMPLATE,
    { textResponse: true }
  );

  const pageTemplate = useMemo(() => parseYaml(pageTemplateString), [
    pageTemplateString,
  ]);

  const loading =
    loadingApuPartTypes ||
    loadingApuPartItemTypes ||
    loadingFacets ||
    loadingPageTemplate ||
    loadingImages;

  useEffect(() => {
    const loadImages = async () => {
      const logo = configuration.showAppLogo ? await getPageTemplateLogo() : null;
      const topImage = await getPageTemplateTopImage();

      if (logo) {
        setAppLogo(logo);
      }

      if (topImage) {
        setAppTopImage(topImage);
      }

      setLoadingImages(false);
    };

    loadImages();
  }, []);

  return !loadingImages ? (
    <AppWrapper {...{ appLogo, appTopImage, pageTemplate }}>
      <>
        <Helmet>
          <meta charSet="utf-8" />
          <title>{pageTemplate?.name || 'Archive Online'}</title>
        </Helmet>
        {apuPartTypes && apuPartItemTypes && facets ? (
          <Switch>
            {getNavigationItems(configuration).map(
              ({ path, exact = false, Component, label }: any) => (
                <Route {...{ key: path, path, exact }}>
                  <Component
                    {...{
                      path,
                      label,
                      apuPartTypes,
                      apuPartItemTypes,
                      facets,
                      appLogo,
                      appTopImage,
                      pageTemplate,
                    }}
                  />
                </Route>
              )
            )}
          </Switch>
        ) : !loading ? (
          <div className={classes.appLoadingFailed}>
            <FormattedMessage id={Message.APP_LOADING_FAILED} />
          </div>
        ) : (
          <Loading {...{ loading }} />
        )}
      </>
    </AppWrapper>
  ) : (
    <Loading {...{ loading }} />
  );
}

function AppWithConfiguration() {
  const classes = useAppStyles();
  const configuration = useConfiguration();

  return <ThemeProvider {...{ primary, editing, highlight, fontSize, fontFamily }}>
    <LocaleProvider
      defaultLocale={getLocaleFromCookie(configuration.localeCookieName) || LocaleName.cs}
      messages={overrideLocale(messages, configuration)}
      translationsUrl=""
    >
      <div className={classes.app}>
        <SnackbarProvider timeout={3000}>
          <AppStateProvider>
            <BrowserRouter {...{ basename: process.env.URL_PREFIX }}>
              <NavigationProvider>
                <AppComponent />
              </NavigationProvider>
            </BrowserRouter>
          </AppStateProvider>
        </SnackbarProvider>
      </div>
    </LocaleProvider>
  </ThemeProvider>
}

export function App() {
  return (
    <ConfigurationProvider>
      <AppWithConfiguration/>
    </ConfigurationProvider>
  );
}
