import { ComponentType } from 'react';
import { GridProps } from 'components/grid/grid-types';

export interface DndGridProps {
  columns: number;
  value: string[];
  onChange: (value: string[]) => void;
  GridComponent?: ComponentType<GridProps>;
  SortableWrapperComponent?: ComponentType<SortableWrapperProps>;
  ItemWrapperComponent?: ComponentType<ItemWrapperProps>;
  ItemComponent: ComponentType<ItemProps>;
}

export interface ItemWrapperProps {
  overlay: boolean;
  style?: React.CSSProperties;
}

export interface SortableWrapperProps {
  id: string;
}

export interface ItemProps {
  id: string;
}
