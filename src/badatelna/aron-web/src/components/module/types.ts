import { ReactChild } from 'react';

import { ModulePath } from '../../enums';

interface Item {
  path?: string;
  label: ReactChild;
}

export interface Breadcrumb extends Item {
  index: number;
  allItems: Item[];
  items?: Item[];
  lastItemWidth: number;
}

export interface Breadcrumbs {
  items: Item[];
  toolbar?: ReactChild;
}

export interface ModuleProps extends Breadcrumbs {
  path?: ModulePath;
  children: ReactChild;
}

export interface Props extends ModuleProps {}
