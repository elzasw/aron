/* eslint-disable react/prop-types */
import React, {
  PropsWithChildren,
  useCallback,
  forwardRef,
  memo,
  Ref,
  ReactElement,
  useRef,
  useImperativeHandle,
  useState,
  useEffect,
} from 'react';
import clsx from 'clsx';
import { unstable_batchedUpdates } from 'react-dom';
import { noop } from 'lodash';
import { VariableSizeList, ListChildComponentProps } from 'react-window';
import InfiniteLoader from 'react-window-infinite-loader';
import ListItem from '@material-ui/core/ListItem';
import ListItemText from '@material-ui/core/ListItemText';
import { Tooltip } from 'components/tooltip/tooltip';
import { InfiniteListProps, InfiniteListHandle } from './infinite-list-types';
import { useStyles } from './infinite-list-styles';
import { DomainObject } from 'common/common-types';
import { useEventCallback } from 'utils/event-callback-hook';
import { composeRefs } from 'utils/compose-refs';

// eslint-disable-next-line react/display-name
export const InfiniteList = memo(
  forwardRef(function InfiniteList<ITEM extends DomainObject>(
    {
      source,
      onItemClick = noop,
      labelMapper = (item: ITEM) => (item as any).name,
      tooltipMapper = (option: ITEM) => (option as any).tooltip,
      showTooltip = false,
      selectedIds = [],
      maxListHeight = 300,
      defaultRowHeight = 26,
    }: InfiniteListProps<ITEM>,
    ref: Ref<InfiniteListHandle<ITEM>>
  ) {
    const classes = useStyles();

    /**
     * Reference to InfiniteLoader
     */
    const loader = useRef<InfiniteLoader>(null);

    /**
     * Reference to VariableSizeList
     */
    const listRef = useRef<VariableSizeList>(null);

    /**
     * Map of each mounted item height.
     */
    const itemHeightMap = useRef<{ [key: number]: number }>({});

    const setItemHeight = useEventCallback((index, size) => {
      itemHeightMap.current = { ...itemHeightMap.current, [index]: size };
    });
    const getItemHeight = useEventCallback(
      (index) => itemHeightMap.current[index] || defaultRowHeight
    );

    /**
     * Currently focused item index. Defaults to none.
     */
    const [focusedIndex, setFocusedIndex] = useState<number>(-1);

    /**
     * Height of VariableSizeList, dynamically changed between `defaultRowHeight` and `maxListHeight`.
     */
    const [listHeight, setListHeight] = useState(defaultRowHeight);

    const computeListHeight = useEventCallback(
      (itemIndex?: number, itemHeight?: number) => {
        if (
          itemIndex !== undefined &&
          itemHeight !== undefined &&
          listRef.current
        ) {
          setItemHeight(itemIndex, itemHeight);
          listRef.current.resetAfterIndex(itemIndex);
        } else {
          itemHeightMap.current = {};
        }

        let newListHeight = 0;
        for (const key in itemHeightMap.current) {
          newListHeight += itemHeightMap.current[key];
        }

        if (newListHeight > maxListHeight) {
          newListHeight = maxListHeight;
        }

        setListHeight(newListHeight);
      }
    );

    useEffect(() => {
      computeListHeight();
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [source.count]);

    /**
     * Infinite list interface.
     */
    useImperativeHandle(
      ref,
      () => ({
        reset: () => {
          unstable_batchedUpdates(() => {
            setFocusedIndex(-1);
            loader.current?.resetloadMoreItemsCache(false);
            source.loadMore();
          });
        },
        focusPrevious: () => {
          const newIndex = Math.min(
            Math.max(focusedIndex - 1, 0),
            source.items.length - 1
          );
          setFocusedIndex(newIndex);
        },
        focusNext: () => {
          const newIndex = Math.min(
            Math.max(focusedIndex + 1, 0),
            source.items.length - 1
          );
          setFocusedIndex(newIndex);
        },
        getFocusedItem: () => {
          return focusedIndex !== -1 ? source.items[focusedIndex] : undefined;
        },
      }),
      [focusedIndex, source]
    );

    const renderItem = useCallback(
      (props: PropsWithChildren<ListChildComponentProps>) => {
        const { index, style } = props;
        const id =
          index < source.items.length ? source.items[index].id : undefined;
        const value =
          index < source.items.length
            ? labelMapper(source.items[index], index)
            : '';
        const tooltip =
          index < source.items.length ? tooltipMapper(source.items[index]) : '';

        const renderedItem = (
          <ListItem
            onClick={() =>
              index < source.items.length &&
              onItemClick(source.items[index], index)
            }
            button
            style={style}
            key={index}
            classes={{
              root: clsx(classes.item, {
                [classes.itemSelected]:
                  id !== undefined && selectedIds.includes(id),
                [classes.itemFocused]: index === focusedIndex,
              }),
            }}
          >
            <ListItemText
              disableTypography={true}
              primary={value}
              ref={(item: HTMLDivElement) => {
                if (
                  item &&
                  item.clientHeight !== itemHeightMap.current[index]
                ) {
                  computeListHeight(index, item.clientHeight);
                }
              }}
            />
          </ListItem>
        );

        if (showTooltip) {
          return <Tooltip title={tooltip}>{renderedItem}</Tooltip>;
        } else {
          return renderedItem;
        }
      },
      [
        source.items,
        labelMapper,
        tooltipMapper,
        classes.item,
        classes.itemSelected,
        classes.itemFocused,
        selectedIds,
        focusedIndex,
        showTooltip,
        onItemClick,
        computeListHeight,
      ]
    );

    const totalCount = source.hasNextPage()
      ? source.items.length + 1
      : source.items.length;

    const isItemLoaded = useCallback(
      (index: number) => {
        return source.isDataValid() && index < source.items.length;
      },
      [source]
    );

    const loadMoreItems = source.loading ? async () => noop() : source.loadMore;

    return (
      <InfiniteLoader
        ref={loader}
        isItemLoaded={isItemLoaded}
        itemCount={totalCount}
        loadMoreItems={loadMoreItems}
        threshold={1}
      >
        {({ onItemsRendered, ref }) => {
          const composedRef = composeRefs(ref, listRef);

          return (
            <VariableSizeList
              height={listHeight}
              width="100%"
              itemSize={getItemHeight}
              itemCount={totalCount}
              estimatedItemSize={defaultRowHeight}
              ref={composedRef}
              onItemsRendered={onItemsRendered}
            >
              {renderItem}
            </VariableSizeList>
          );
        }}
      </InfiniteLoader>
    );
  })
) as <ITEM extends DomainObject>(
  p: InfiniteListProps<ITEM> & { ref?: Ref<InfiniteListHandle<ITEM>> }
) => ReactElement;
