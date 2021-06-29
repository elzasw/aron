import React from 'react';
import { FormattedMessage } from 'react-intl';

import { ModulePath } from './module';
import { Evidence, Main, News } from '../modules';
import { ApuType } from './apu';
import { FavouriteQuery } from '../types';
import { Message } from './message';

export const appHeaderItems = [
  {
    path: ModulePath.FUND,
    label: <FormattedMessage id={Message.FUND} />,
    Component: Evidence,
  },
  {
    path: ModulePath.FINDING_AID,
    label: <FormattedMessage id={Message.FINDING_AID} />,
    Component: Evidence,
  },
  {
    path: ModulePath.ARCH_DESC,
    label: <FormattedMessage id={Message.ARCH_DESC} />,
    Component: Evidence,
  },
  {
    path: ModulePath.ENTITY,
    label: <FormattedMessage id={Message.ENTITY} />,
    Component: Evidence,
  },
  {
    path: ModulePath.NEWS,
    label: <FormattedMessage id={Message.NEWS} />,
    Component: News,
  },
  {
    label: <FormattedMessage id={Message.HELP} />,
    url:
      'https://vychodoceskearchivy.cz/home/prezentace-archivu/e-vystava-archivalii/archiv-online-napoveda',
  },
];

export const navigationItems = [
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
  },
  ...appHeaderItems.filter(({ Component }) => Component),
];

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
