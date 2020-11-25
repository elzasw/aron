import React, {
  useCallback,
  PropsWithChildren,
  memo,
  ReactElement,
  forwardRef,
  Ref,
  RefAttributes,
} from 'react';
import useMeasure from 'react-use-measure';
import { FixedSizeList, ListChildComponentProps } from 'react-window';
import InfiniteLoader from 'react-window-infinite-loader';
import LinearProgress from '@material-ui/core/LinearProgress';
import { TableProps, TableHandle } from './table-types';
import { useStyles } from './table-styles';
import { useTable } from './table-hook';
import { TableContext, TableSelectedContext } from './table-context';
import { DomainObject } from 'common/common-types';
import { composeRefs } from 'utils/compose-refs';
import { useScrollBarSize } from 'utils/use-scrollbar-size';

// eslint-disable-next-line react/display-name
export const Table = memo(
  forwardRef(function Table<OBJECT extends DomainObject>(
    options: TableProps<OBJECT>,
    ref: Ref<TableHandle<OBJECT>>
  ) {
    const classes = useStyles();

    const {
      props,
      context,
      selectedContext,
      columnDialogRef,
      filterDialogRef,
      loaderRef,
      listRef,
      handleKeyNavigation,
    } = useTable<OBJECT>(options, ref);

    // eslint-disable-next-line react/prop-types
    const {
      SearchbarComponent,
      ToolbarComponent,
      ColumnDialogComponent,
      FilterDialogComponent,
      HeaderComponent,
      RowComponent,
      source,
      height,
      toolbarProps,
    } = props;

    const isItemLoaded = useCallback(
      (index: number) => {
        return source.isDataValid() && index < source.items.length;
      },
      [source]
    );

    const renderItem = useCallback(
      (props: PropsWithChildren<ListChildComponentProps>) => {
        const { index, style } = props;

        return (
          <div style={style}>
            {index < source.items.length ? (
              <RowComponent
                value={source.items[index]}
                index={index}
                selected={false}
              />
            ) : (
              <></>
            )}
          </div>
        );
      },
      [source.items]
    );

    const [barMeasureRef, { height: barHeight }] = useMeasure({
      debounce: 200,
    });

    // we need real width of the header for the react-window wrapper
    const [
      headerMeasureRef,
      { width: headerWidth, height: headerHeight },
    ] = useMeasure({
      debounce: 200,
    });

    const totalCount = source.hasNextPage()
      ? source.items.length + 1
      : source.items.length;

    const scrollbarHeight = useScrollBarSize();
    const listHeight = height - barHeight - headerHeight - scrollbarHeight;

    return (
      <TableContext.Provider value={context}>
        <TableSelectedContext.Provider value={selectedContext}>
          <div className={classes.wrapper}>
            <div className={classes.tableGroupWrapper}>
              <div ref={barMeasureRef}>
                <div className={classes.searchWrapper}>
                  <SearchbarComponent />
                </div>
                <ToolbarComponent {...toolbarProps} />
              </div>

              <div
                className={classes.tableWrapper}
                tabIndex={0}
                onKeyDown={handleKeyNavigation}
              >
                {source.loading && (
                  <LinearProgress classes={{ root: classes.progress }} />
                )}
                <HeaderComponent ref={headerMeasureRef} />
                <div
                  className={classes.dataWrapper}
                  style={{ width: headerWidth }}
                >
                  <InfiniteLoader
                    ref={loaderRef}
                    isItemLoaded={isItemLoaded}
                    itemCount={totalCount}
                    loadMoreItems={source.loadMore}
                  >
                    {({ onItemsRendered, ref }) => {
                      const composedRef = composeRefs(ref, listRef);
                      return (
                        <FixedSizeList
                          height={listHeight}
                          width="100%"
                          itemSize={30}
                          itemCount={totalCount}
                          ref={composedRef}
                          onItemsRendered={onItemsRendered}
                          className={classes.scrollContainer}
                          direction="rtl"
                        >
                          {renderItem}
                        </FixedSizeList>
                      );
                    }}
                  </InfiniteLoader>
                </div>
              </div>
            </div>
            <ColumnDialogComponent ref={columnDialogRef} />
            <FilterDialogComponent ref={filterDialogRef} />
          </div>
        </TableSelectedContext.Provider>
      </TableContext.Provider>
    );
  })
) as <OBJECT extends DomainObject>(
  p: TableProps<OBJECT> & RefAttributes<TableHandle<OBJECT>>
) => ReactElement;
