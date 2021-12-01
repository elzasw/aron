import React, { ComponentType, ReactChild } from 'react';
import { FormattedMessage } from 'react-intl';

import { ModulePath } from './module';
import { Evidence, Main, News } from '../modules';
import { ApuType } from './apu';
import { FavouriteQuery } from '../types';
import { Message } from './message';
import { ConfigurationType } from '../components'

export enum AppHeaderItemCode {
  ARCHIVE = "ARCHIVE",
  FUND = "FUND",
  FINDING_AID = "FINDING_AID",
  ARCH_DESC = "ARCH_DESC",
  ENTITY = "ENTITY",
  ORIGINATOR = "ORIGINATOR",
  NEWS = "NEWS",
  HELP = "HELP",
}

export interface NavigationItem {
    exact?: boolean;
    path?: string;
    label: ReactChild;
    Component?: ComponentType<any>;
    url?: string;
}

export const appHeaderItems:Record<AppHeaderItemCode, NavigationItem> = {
  ARCHIVE: {
    path: ModulePath.ARCHIVE,
    label: <FormattedMessage id={Message.ARCHIVES} />,
    Component: Evidence,
  },
  FUND: {
    path: ModulePath.FUND,
    label: <FormattedMessage id={Message.FUND} />,
    Component: Evidence,
  },
  FINDING_AID: {
    path: ModulePath.FINDING_AID,
    label: <FormattedMessage id={Message.FINDING_AID} />,
    Component: Evidence,
  },
  ARCH_DESC: {
    path: ModulePath.ARCH_DESC,
    label: <FormattedMessage id={Message.ARCH_DESC} />,
    Component: Evidence,
  },
  ENTITY: {
    path: ModulePath.ENTITY,
    label: <FormattedMessage id={Message.ENTITY} />,
    Component: Evidence,
  },
  ORIGINATOR: {
    path: ModulePath.ORIGINATOR,
    label: <FormattedMessage id={Message.ORIGINATORS} />,
    Component: Evidence,
  },
  NEWS: {
    path: ModulePath.NEWS,
    label: <FormattedMessage id={Message.NEWS} />,
    Component: News,
  },
  HELP: {
    label: <FormattedMessage id={Message.HELP} />,
    url:
      'https://vychodoceskearchivy.cz/home/prezentace-archivu/e-vystava-archivalii/archiv-online-napoveda',
  },
};

export const navigationItems:NavigationItem[] = [
  {
    exact: true,
    path: ModulePath.MAIN,
    label: <FormattedMessage id={Message.INTRODUCTION} />,
    Component: Main,
  },
  {
    path: ModulePath.APU,
    label: <FormattedMessage id={Message.SEARCH} />,
    Component: Evidence,
  }
];


export const getAppHeaderItems = (configuration: ConfigurationType) => {
    const headerItems = configuration.headerItems || [];
    return headerItems.map((code)=>{
        return appHeaderItems[code]
    })
}

export const getNavigationItems = (configuration: ConfigurationType) => {
    return [
        ...navigationItems,
        ...getAppHeaderItems(configuration).filter(({Component}) => Component),
    ]
}

export const favouriteQueries: FavouriteQuery[] = [
  {
    icon: 'fas fa-folder-open',
    label: 'České sbírky',
    type: ApuType.FUND,
    query: 'Sbírky',
    filters: [
      {
        source: 'LANGUAGE',
        value: ['Česky'],
      },
    ],
  },
  { icon: 'fas fa-graduation-cap', label: 'Spisy žáků/studentů' },
  { icon: 'fas fa-book', label: 'Kroniky úřední' },
  { icon: 'fas fa-users', label: 'Spisy evidence obyvatelstva' },
  { icon: 'fas fa-book', label: 'Kroniky neúřední' },
  { icon: 'fas fa-id-badge', label: 'Úřední kihy/evidence obyvatelstva' },
  { icon: 'fas fa-building', label: 'Stavební spisy' },
  { icon: 'fas fa-id-badge', label: 'Kartotéky evidence obyvatelstva' },
  { icon: 'fas fa-graduation-cap', label: 'Třídní výkazy' },
  { icon: 'fas fa-users', label: 'Sčítání lidu' },
  { icon: 'fas fa-map', label: 'Technické výkresy staveb' },
  { icon: 'fas fa-folder-open', label: 'Spis evidující nemovistosti' },
];
