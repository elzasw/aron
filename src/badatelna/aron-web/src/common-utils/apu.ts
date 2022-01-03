import { ModulePath, ApuType } from '../enums';
import { find } from 'lodash';
import { ApuPartItemType, ApuEntity } from '../types';

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
    case ModulePath.INSTITUTION:
      return ApuType.INSTITUTION;
    case ModulePath.ORIGINATOR:
      return ApuType.ENTITY;
    default:
      return null;
  }
};

export const getPathByItem = (item: ApuEntity) => {
  const type = item.type;
  switch (type) {
    case ApuType.FUND:
    case ApuType.FINDING_AID:
    case ApuType.ARCH_DESC:
    case ApuType.INSTITUTION:
      return getPathByType(type);
    case ApuType.ENTITY:
      if(isApuOriginator(item)) {
        return ModulePath.ORIGINATOR
      }
      return ModulePath.ENTITY;
    default:
      return ModulePath.APU;
  }
}

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
    case ApuType.INSTITUTION:
      return ModulePath.INSTITUTION;
    default:
      return ModulePath.APU;
  }
};

export const isApuOriginator = (item: ApuEntity) => {
  return item.parts?.find((part)=>
    part.items.find((partItem)=>
      partItem.type === 'AE~ORIGINATOR' &&
      partItem.value === 'ANO'
    )
  ) ? true : false;
}

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
