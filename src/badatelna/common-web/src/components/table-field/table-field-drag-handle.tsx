import React from 'react';
import { SortableHandle } from 'react-sortable-hoc';
import DragIndicatorIcon from '@material-ui/icons/DragIndicator';

/**
 * Draggable handle to change row order.
 */
export const DragHandle = SortableHandle(
  ({ className }: { className?: string }) => (
    <DragIndicatorIcon className={className} />
  )
);
