import React, {
  PropsWithChildren,
  useCallback,
  forwardRef,
  Ref,
  useImperativeHandle,
  ReactElement,
  RefAttributes,
} from 'react';
import useMeasure from 'react-use-measure';
import { FixedSizeList, ListChildComponentProps } from 'react-window';
import Grid from '@material-ui/core/Grid';
import { TableFieldProps, TableFieldHandle } from './table-field-types';
import { useStyles } from './table-field-styles';
import { TableFieldContext } from './table-field-context';
import { useTableField } from './table-field-hook';

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

  const height = props.maxRows * 30;

  const renderItem = useCallback(
    ({ index, style, data }: PropsWithChildren<ListChildComponentProps>) => {
      const { value, selectedIndex } = data;

      return (
        <div style={style}>
          <RowComponent
            value={value[index]}
            index={index}
            selected={selectedIndex === index}
          />
        </div>
      );
    },
    []
  );

  useImperativeHandle(
    ref,
    () => ({
      selectedIndex,
      setSelectedIndex: (index: number) => {
        setSelectedIndex(index);
      },
    }),
    [selectedIndex, setSelectedIndex]
  );

  // we need real width of the header for the react-window wrapper
  const [measureRef, { width }] = useMeasure();

  return (
    <TableFieldContext.Provider value={context}>
      <div className={classes.componentWrapper}>
        {showToolbar && <ToolbarComponent selectedIndex={selectedIndex} />}
        <Grid container className={classes.grid}>
          <div className={classes.tableWrapper}>
            <HeaderComponent ref={measureRef} />
            <div className={classes.dataWrapper} style={{ height, width }}>
              <FixedSizeList
                height={height}
                width="100%"
                itemSize={30}
                itemData={{
                  value: context.value,
                  selectedIndex,
                }}
                itemCount={context.value.length}
              >
                {renderItem}
              </FixedSizeList>
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
