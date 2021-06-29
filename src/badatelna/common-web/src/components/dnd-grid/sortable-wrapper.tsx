import React, { PropsWithChildren } from 'react';
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { ItemWrapper } from './item-wrapper';
import { SortableWrapperProps } from './dnd-grid-types';

export function SortableWrapper(
  props: PropsWithChildren<SortableWrapperProps>
) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
  } = useSortable({ id: props.id });

  const style: React.CSSProperties = {
    transform: CSS.Transform.toString(transform),
    transition: transition as any,
  };

  return (
    <ItemWrapper
      overlay={false}
      ref={setNodeRef}
      style={style}
      {...props}
      {...attributes}
      {...listeners}
    />
  );
}
