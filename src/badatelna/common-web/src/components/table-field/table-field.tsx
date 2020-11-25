import React, { PropsWithChildren, useCallback } from 'react';
import useMeasure from 'react-use-measure';
import { FixedSizeList, ListChildComponentProps } from 'react-window';
import Grid from '@material-ui/core/Grid';
import { TableFieldProps } from './table-field-types';
import { useStyles } from './table-field-styles';
import { TableFieldContext } from './table-field-context';
import { useTableField } from './table-field-hook';

export function TableField<OBJECT>(options: TableFieldProps<OBJECT>) {
  const classes = useStyles();

  const {
    props,
    selectedIndex,
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

  const height = props.maxRows * 25;

  const renderItem = useCallback(
    (props: PropsWithChildren<ListChildComponentProps>) => {
      const { index, style } = props;

      return (
        <div style={style}>
          <RowComponent
            value={context.value[index]}
            index={index}
            selected={selectedIndex === index}
          />
        </div>
      );
    },
    [selectedIndex, context.value]
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
                itemSize={25}
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
        {selectedIndex !== undefined && (
          <RemoveDialogComponent ref={removeDialogRef} index={selectedIndex} />
        )}
      </div>
    </TableFieldContext.Provider>
  );
}
