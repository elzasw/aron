import { useState, RefObject, useContext } from 'react';
import { FixedSizeList } from 'react-window';
import { useEventCallback } from 'utils/event-callback-hook';
import { DomainObject, ScrollableSource } from 'common/common-types';
import { NavigationContext } from 'composite/navigation/navigation-context';

export function useTableSelect<OBJECT extends DomainObject>({
  source,
  listRef,
  onActiveChange,
}: {
  source: ScrollableSource<OBJECT>;
  listRef: RefObject<FixedSizeList>;
  onActiveChange: (id: string | null) => void;
}) {
  const { testPrompts } = useContext(NavigationContext);

  /**
   * Selected item ids.
   */
  const [selected, setSelected] = useState<string[]>([]);

  const [activeRow, setActiveRowInternal] = useState<string | null>(null);

  /**
   * Toggles between selecting all rows and no rows.
   *
   * Only visible (loaded) rows can be selected.
   * If at least one row is selected, next toggle will deselect all.
   */
  const toggleAllRowSelection = useEventCallback(() =>
    setSelected((selected) =>
      selected.length > 0 ? [] : source.items.map((row) => row.id)
    )
  );

  /**
   * Toggles between selecting one row.
   */
  const toggleRowSelection = useEventCallback((id: string) => {
    setSelected((selected) =>
      selected.includes(id)
        ? selected.filter((r) => r !== id)
        : [...selected, id]
    );
  });

  /**
   * Resets selection.
   */
  const resetSelection = useEventCallback(() => {
    setSelected([]);
  });

  const setActiveRow = useEventCallback((id: string | null) => {
    if (id !== activeRow) {
      testPrompts(() => {
        setActiveRowInternal(id);
        onActiveChange(id);
      });
    }
  });

  const setNextRowActive = useEventCallback(() => {
    const count = source.items.length;
    if (count > 0) {
      const activeIndex = source.items.findIndex(
        (item) => item.id === activeRow
      );
      const nextIndex = Math.min(activeIndex + 1, count - 1);
      setActiveRow(source.items[nextIndex].id);
      listRef.current?.scrollToItem(nextIndex);
    }
  });

  const setPrevRowActive = useEventCallback(() => {
    const count = source.items.length;
    if (count > 0) {
      const activeIndex = source.items.findIndex(
        (item) => item.id === activeRow
      );
      const prevIndex = Math.max(activeIndex - 1, 0);
      setActiveRow(source.items[prevIndex].id);
      listRef.current?.scrollToItem(prevIndex);
    }
  });

  return {
    selected,
    activeRow,
    toggleAllRowSelection,
    toggleRowSelection,
    resetSelection,
    setActiveRow,
    setNextRowActive,
    setPrevRowActive,
  };
}
