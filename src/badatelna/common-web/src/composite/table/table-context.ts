import { createContext } from 'react';
import { TableHandle } from './table-types';

export type TableContext<OBJECT> = TableHandle<OBJECT>;

export const TableContext = createContext<TableContext<any>>(undefined as any);

export interface TableSelectedContext {
  selected: string[];
  activeRow: string | null;
}

export const TableSelectedContext = createContext<TableSelectedContext>(
  undefined as any
);
