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
} from 'react';
import clsx from 'clsx';
import { noop } from 'lodash';
import { FixedSizeList, ListChildComponentProps } from 'react-window';
import AutoSizer from 'react-virtualized-auto-sizer';
import InfiniteLoader from 'react-window-infinite-loader';
import ListItem from '@material-ui/core/ListItem';
import ListItemText from '@material-ui/core/ListItemText';
import { Tooltip } from 'components/tooltip/tooltip';
import { InfiniteListProps, InfiniteListHandle } from './infinite-list-types';
import { useStyles } from './infinite-list-styles';
import { DomainObject } from 'common/common-types';
import { useForceRender } from 'utils/force-render';

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
    }: InfiniteListProps<ITEM>,
    ref: Ref<InfiniteListHandle<ITEM>>
  ) {
    const classes = useStyles();
    const loader = useRef<InfiniteLoader>(null);
    const [focusedIndex, setFocusedIndex] = useState<number>(-1);
    const { forceRender } = useForceRender();

    useImperativeHandle(
      ref,
      () => ({
        reset: () => {
          forceRender();
          loader.current?.resetloadMoreItemsCache(true);
          setFocusedIndex(-1);
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
      [focusedIndex, forceRender, source.items]
    );

    const renderItem = useCallback(
      (props: PropsWithChildren<ListChildComponentProps>) => {
        const { index, style } = props;
        const id =
          index < source.items.length ? source.items[index].id : undefined;
        const value =
          index < source.items.length ? labelMapper(source.items[index]) : '';
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
              classes={{ root: classes.itemText }}
              disableTypography={true}
              primary={value}
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
        classes.itemFocused,
        classes.itemSelected,
        classes.itemText,
        focusedIndex,
        selectedIds,
        showTooltip,
        onItemClick,
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

    return (
      <AutoSizer>
        {({ width, height }) => {
          return (
            <InfiniteLoader
              ref={loader}
              isItemLoaded={isItemLoaded}
              itemCount={totalCount}
              loadMoreItems={source.loadMore}
            >
              {({ onItemsRendered, ref }) => {
                return (
                  <FixedSizeList
                    height={height}
                    width={width}
                    itemSize={26.5}
                    itemCount={totalCount}
                    ref={ref}
                    onItemsRendered={onItemsRendered}
                  >
                    {renderItem}
                  </FixedSizeList>
                );
              }}
            </InfiniteLoader>
          );
        }}
      </AutoSizer>
    );
  })
) as <ITEM extends DomainObject>(
  p: InfiniteListProps<ITEM> & { ref?: Ref<InfiniteListHandle<ITEM>> }
) => ReactElement;
