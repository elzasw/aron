import React from 'react';
import { ApiFilterOperation } from '../types';
import { Message } from '.';
import { FormattedMessage } from 'react-intl';

export enum ModulePath {
  MAIN = '/',
  APU = '/apu',
  FUND = '/fund',
  FINDING_AID = '/finding-aid',
  ARCH_DESC = '/arch-desc',
  ENTITY = '/entity',
  NEWS = '/news',
  HELP = '/help',
}

export const searchOptions = [
  {
    name: <FormattedMessage id={Message.ARCH_DESC_DAO_ONLY} />,
    path: ModulePath.ARCH_DESC,
    filters: [
      {
        field: 'containsDigitalObjects',
        operation: ApiFilterOperation.EQ,
        type: null,
        label: '_',
        options: [{ id: 0, value: true }],
        value: [0],
      },
    ],
  },
  {
    name: <FormattedMessage id={Message.ARCH_DESC} />,
    path: ModulePath.ARCH_DESC,
  },
  { name: <FormattedMessage id={Message.FUND} />, path: ModulePath.FUND },
  {
    name: <FormattedMessage id={Message.FINDING_AID} />,
    path: ModulePath.FINDING_AID,
  },
  { name: <FormattedMessage id={Message.ENTITY} />, path: ModulePath.ENTITY },
];
