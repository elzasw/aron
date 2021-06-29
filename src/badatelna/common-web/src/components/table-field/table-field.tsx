import React, {
  PropsWithChildren,
  useCallback,
  forwardRef,
  Ref,
  useImperativeHandle,
  ReactElement,
  RefAttributes,
  useMemo,
  useRef,
  useState,
  useEffect,
} from 'react';
import clsx from 'clsx';
import { SortableContainer, SortableElement } from 'react-sortable-hoc';
import useMeasure from 'react-use-measure';
import { VariableSizeList, ListChildComponentProps } from 'react-window';
import Grid from '@material-ui/core/Grid';
import { TableFieldProps, TableFieldHandle } from './table-field-types';
import { useStyles } from './table-field-styles';
import { TableFieldContext } from './table-field-context';
import { useTableField } from './table-field-hook';
import { useEventCallback } from 'utils/event-callback-hook';

export const TableField = forwardRef(function TableField<OBJECT>(
  options: TableFieldProps<OBJECT>,
  ref: Ref<TableFieldHandle>
) {
  const classes = useStyles();

  const {
    props,
    selectedIndex,
    setSelectedIndex,
    context,
    formDialogRef,
    removeDialogRef,
    swapRows,
  } = useTableField(options);

  const {
    RowComponent,
    showToolbar,
    ToolbarComponent,
    HeaderComponent,
    FormFieldsComponent,
    DialogComponent,
    RemoveDialogComponent,
  } = props;

  /**
   * Reference to VariableSizeList
   */
  const listRef = useRef<VariableSizeList>(null);

  const SortableFixedSizeList = useMemo(
    () =>
      SortableContainer((props: any) => (
        <VariableSizeList {...props} ref={listRef} />
      )),
    []
  );

  /**
   * Map of each mounted item height.
   */
  const itemHeightMap = useRef<{ [key: number]: number }>({});

  const maxHeight = props.maxRows * 30;
  const minWidth = props.columns
    .map((col) => col.width)
    .reduce((a, b) => a + b, 0);

  const setItemHeight = useEventCallback((index, size) => {
    itemHeightMap.current = { ...itemHeightMap.current, [index]: size };
  });
  const getItemHeight = useEventCallback(
    (index) => itemHeightMap.current[index] || 30
  );

  /**
   * Height of VariableSizeList, dynamically changed between `defaultRowHeight` and `maxListHeight`.
   */
  const [listHeight, setListHeight] = useState(30);

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

      if (newListHeight > maxHeight) {
        newListHeight = maxHeight;
      }

      setListHeight(newListHeight);
    }
  );

  useEffect(() => {
    computeListHeight();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [context.value.length]);

  const SortableItem = SortableElement(
    (props: { nestedProps: ListChildComponentProps }) => {
      const { index, style, data } = props.nestedProps;
      const { value, selectedIndex } = data;

      return (
        <div style={{ ...style }}>
          <RowComponent
            value={value[index]}
            index={index}
            selected={selectedIndex === index}
            ref={(item: HTMLDivElement) => {
              if (
                item &&
                item.clientHeight + 1 !== itemHeightMap.current[index]
              ) {
                computeListHeight(index, item.clientHeight + 1);
              }
            }}
          />
        </div>
      );
    }
  );

  const renderItem = useCallback(
    (props: PropsWithChildren<ListChildComponentProps>) => {
      const { index } = props;

      // we need to post the props inside, so the index prop is not discarted by 'react-sortable-hoc'
      return <SortableItem index={index} nestedProps={props} />;
    },
    []
  );

  useImperativeHandle(
    ref,
    () => ({
      selectedIndex,
      setSelectedIndex: (index: number | undefined) => {
        setSelectedIndex(index);
      },
    }),
    [selectedIndex, setSelectedIndex]
  );

  // we need real width of the header for the react-window wrapper
  const [measureRef, { width }] = useMeasure();

  return (
    <TableFieldContext.Provider value={context}>
      <div
        className={clsx(classes.componentWrapper, {
          [classes.noMinHeigth]: options.noMinHeigth,
        })}
        style={{
          width: options.noMinHeigth ? minWidth : 'auto',
        }}
      >
        {showToolbar && (
          <ToolbarComponent
            selectedIndex={selectedIndex}
            setSelectedIndex={setSelectedIndex}
          />
        )}
        <Grid
          container
          className={clsx(classes.grid, {
            [classes.noMinHeigth]: options.noMinHeigth,
          })}
        >
          <div className={classes.tableWrapper}>
            <HeaderComponent ref={measureRef} />
            <div
              className={clsx(classes.dataWrapper, {
                [classes.noMinHeigth]: options.noMinHeigth,
              })}
              style={{ maxHeight, width }}
            >
              <SortableFixedSizeList
                useDragHandle
                onSortEnd={swapRows}
                height={Math.min(maxHeight, listHeight)}
                width="100%"
                style={{
                  overflowX: 'hidden',
                  overflowY: 'auto',
                }}
                itemSize={getItemHeight}
                itemData={{
                  value: context.value,
                  selectedIndex,
                }}
                itemCount={context.value.length}
              >
                {renderItem}
              </SortableFixedSizeList>
            </div>
          </div>
        </Grid>
        {FormFieldsComponent !== undefined && (
          <DialogComponent
            ref={formDialogRef}
            index={selectedIndex}
            FormFieldsComponent={FormFieldsComponent}
            value={
              selectedIndex !== undefined ? context.value[selectedIndex] : null
            }
          />
        )}
        <RemoveDialogComponent ref={removeDialogRef} index={selectedIndex} />
      </div>
    </TableFieldContext.Provider>
  );
}) as <OBJECT>(
  p: TableFieldProps<OBJECT> & RefAttributes<TableFieldHandle>
) => ReactElement;
