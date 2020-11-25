import { ReactNode } from 'react';
import { ScrollableSource, DomainObject } from 'common/common-types';

export interface InfiniteListProps<ITEM extends DomainObject> {
  source: ScrollableSource<ITEM>;

  onItemClick?: (option: ITEM, index: number) => void;

  labelMapper?: (option: ITEM) => ReactNode;
  tooltipMapper?: (option: ITEM) => ReactNode;

  showTooltip?: boolean;

  selectedIds?: string[];
}

export interface InfiniteListHandle<ITEM extends DomainObject> {
  reset: () => void;
  focusPrevious: () => void;
  focusNext: () => void;
  getFocusedItem: () => ITEM | undefined;
}
