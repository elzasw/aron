import React, { useState } from 'react';
import {
  DndContext,
  closestCenter,
  MouseSensor,
  TouchSensor,
  DragOverlay,
  useSensor,
  useSensors,
  DragStartEvent,
  DragEndEvent,
} from '@dnd-kit/core';
import {
  arrayMove,
  SortableContext,
  rectSortingStrategy,
} from '@dnd-kit/sortable';
import { Grid } from 'components/grid/grid';
import { useEventCallback } from 'utils/event-callback-hook';
import { DndGridProps } from './dnd-grid-types';
import { SortableWrapper } from './sortable-wrapper';
import { ItemWrapper } from './item-wrapper';

export function DndGrid({
  value,
  onChange,
  columns,
  GridComponent = Grid,
  SortableWrapperComponent = SortableWrapper,
  ItemWrapperComponent = ItemWrapper,
  ItemComponent,
}: DndGridProps) {
  const [activeItem, setActiveItem] = useState<string | null>(null);
  const sensors = useSensors(
    useSensor(MouseSensor, {
      activationConstraint: { delay: 250, tolerance: 5 },
    }),
    useSensor(TouchSensor)
  );

  const handleDragStart = useEventCallback((event: DragStartEvent) => {
    setActiveItem(event.active.id);
  });

  const handleDragEnd = useEventCallback((event: DragEndEvent) => {
    const { active, over } = event;

    if (active.id !== over?.id && over != null) {
      const oldIndex = value.indexOf(active.id);
      const newIndex = value.indexOf(over.id);

      onChange(arrayMove(value, oldIndex, newIndex));
    }

    setActiveItem(null);
  });

  const handleDragCancel = useEventCallback(() => {
    setActiveItem(null);
  });

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCenter}
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
      onDragCancel={handleDragCancel}
    >
      <SortableContext items={value} strategy={rectSortingStrategy}>
        <GridComponent columns={columns}>
          {value.map((item) => (
            <SortableWrapperComponent key={item} id={item}>
              <ItemComponent id={item} />
            </SortableWrapperComponent>
          ))}
        </GridComponent>
      </SortableContext>

      <DragOverlay adjustScale={false}>
        {activeItem ? (
          <ItemWrapperComponent overlay>
            <ItemComponent id={activeItem} />
          </ItemWrapperComponent>
        ) : null}
      </DragOverlay>
    </DndContext>
  );
}
