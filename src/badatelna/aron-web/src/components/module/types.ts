import { ReactChild } from 'react';
import { ModulePath } from '../../enums';

export interface ModuleProps {
  children: ReactChild;
  items: { path?: ModulePath; label: string }[];
}

export interface Props extends ModuleProps {}
