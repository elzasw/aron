import React from 'react';
import { FormattedMessage } from 'react-intl';

import { ModulePath } from './module';
import { Evidence, Help, Main, News } from '../modules';
import { ApuType } from './apu';
import { IconType } from './icon';
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
    path: ModulePath.HELP,
    label: <FormattedMessage id={Message.HELP} />,
    Component: Help,
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
  ...appHeaderItems,
];

export const favouriteQueries: FavouriteQuery[] = [
  {
    icon: IconType.FOLDER_OPEN_SOLID,
    label: 'České sbírky',
    type: ApuType.FUND,
    query: 'Sbírky',
    filters: [
      {
        field: 'LANGUAGE',
        value: ['Česky'],
      },
    ],
  },
  { icon: IconType.GRADUATION_CAP_SOLID, label: 'Spisy žáků/studentů' },
  { icon: IconType.BOOK_SOLID, label: 'Kroniky úřední' },
  { icon: IconType.USERS_SOLID, label: 'Spisy evidence obyvatelstva' },
  { icon: IconType.BOOK_SOLID, label: 'Kroniky neúřední' },
  { icon: IconType.ID_BADGE_SOLID, label: 'Úřední kihy/evidence obyvatelstva' },
  { icon: IconType.BUILDING_SOLID, label: 'Stavební spisy' },
  { icon: IconType.ID_BADGE_SOLID, label: 'Kartotéky evidence obyvatelstva' },
  { icon: IconType.GRADUATION_CAP_SOLID, label: 'Třídní výkazy' },
  { icon: IconType.USERS_SOLID, label: 'Sčítání lidu' },
  { icon: IconType.MAP_SOLID, label: 'Technické výkresy staveb' },
  { icon: IconType.FOLDER_OPEN_SOLID, label: 'Spis evidující nemovistosti' },
];
