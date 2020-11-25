import React from 'react';

import { ModulePath, ApuType, FilterType } from '../../enums';
import { SelectionFilter } from './sidebar-content/selection-filter';
import { FilterChangeCallBack } from './types';
import InputboxFilter from './sidebar-content/inputbox-filter';

export const findApuParts = (items: any[], code: string) =>
  items.filter(({ type }) => type === code);

export const filterApuPartTypes = (items: any[], entityItems: any[]) =>
  items.filter(({ code }) => findApuParts(entityItems, code).length);

export const getFilterComponent = ({
  type,
  index = 0,
  onChange = () => null,
  ...props
}: {
  type: FilterType;
  index: number;
  onChange: FilterChangeCallBack;
}) => {
  let FilterComponent: React.ReactType;
  switch (type) {
    case FilterType.SELECT:
    case FilterType.RADIOBUTTON:
    case FilterType.CHECKBOX_WITH_RANGE:
    case FilterType.CHECKBOX:
      FilterComponent = SelectionFilter;
      break;
    case FilterType.INPUTBOX:
      FilterComponent = InputboxFilter;
      break;
    default:
      FilterComponent = () => null;
      break;
  }
  return <FilterComponent {...{ ...props, type, onChange }} key={index} />;
};

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
    default:
      return null;
  }
};

export const getPathByType = (type: ApuType) => {
  switch (type) {
    case ApuType.ENTITY:
      return ModulePath.ENTITY;
    case ApuType.FINDING_AID:
      return ModulePath.FINDING_AID;
    case ApuType.ARCH_DESC:
      return ModulePath.ARCH_DESC;
    default:
      return ModulePath.FUND;
  }
};
