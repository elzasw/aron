import { ReactChild } from 'react';

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
  children: ReactChild;
}

export interface Props extends ModuleProps {}
