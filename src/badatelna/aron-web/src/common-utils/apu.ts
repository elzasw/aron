import { ModulePath, ApuType } from '../enums';
import { find } from 'lodash';
import { ApuPartItemType } from '../types';

export const getTypeByPath = (path: ModulePath) => {
  switch (path) {
    case ModulePath.FUND:
      return ApuType.FUND;
    case ModulePath.ENTITY:
      return ApuType.ENTITY;
    case ModulePath.FINDING_AID:
      return ApuType.FINDING_AID;
    case ModulePath.ARCH_DESC:
      return ApuType.ARCH_DESC;
    case ModulePath.ARCHIVE:
      return ApuType.ENTITY;
    case ModulePath.ORIGINATOR:
      return ApuType.ENTITY;
    default:
      return null;
  }
};

export const getPathByType = (type: ApuType) => {
  switch (type) {
    case ApuType.FUND:
      return ModulePath.FUND;
    case ApuType.ENTITY:
      return ModulePath.ENTITY;
    case ApuType.FINDING_AID:
      return ModulePath.FINDING_AID;
    case ApuType.ARCH_DESC:
      return ModulePath.ARCH_DESC;
    default:
      return ModulePath.APU;
  }
};

export const getApuPartItemName = (
  apuPartItemTypes: ApuPartItemType[],
  code: string
) => find(apuPartItemTypes, { code })?.name;

export const getApuPartItemType = (
  apuPartItemTypes: ApuPartItemType[],
  code: string
) => find(apuPartItemTypes, { code })?.type;

export const parseApuRefOptionId = (option: string) => option.split('|')[0];

export const parseApuRefOptionLabel = (option: string) => option.split('|')[1];
