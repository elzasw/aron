import React, { RefObject, ComponentType, useMemo } from 'react';
import { useIntl } from 'react-intl';
import { noop, stubFalse } from 'lodash';
import {
  TableFieldColumn,
  TableFieldCellProps,
} from 'components/table-field/table-field-types';
import { Form } from 'composite/form/form';
import { FormDateTimeField } from 'composite/form/fields/form-date-time-field';
import { FormTableField } from 'composite/form/fields/form-table-field';
import { FormHandle } from 'composite/form/form-types';
import { Report, ReportColumn, ReportColumnType } from '../reporting-types';
import { TableFieldCells } from 'components/table-field/table-field-cells';
import { DictionaryAutocomplete } from 'common/common-types';
import { useStaticListSource } from 'utils/list-source-hook';

export function ReportData({
  report,
  formRef,
}: {
  report: Report | null;
  formRef: RefObject<FormHandle<Report | null>>;
}) {
  const intl = useIntl();

  const columns: TableFieldColumn<any>[] = (report?.columns ?? []).map((c) =>
    // eslint-disable-next-line react-hooks/rules-of-hooks
    useTableFieldColumn(c)
  );

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const memoizedColumns = useMemo(() => columns, [report]);

  if (report === null) {
    return <></>;
  }

  return (
    <>
      <Form<any>
        ref={formRef}
        initialValues={{} as any}
        onSubmit={noop}
        editing={false}
      >
        {report.created !== undefined && (
          <FormDateTimeField
            label={intl.formatMessage({
              id: 'EAS_REPORTING_DATA_FIELD_CREATED',
              defaultMessage: 'Generováno',
            })}
            name="created"
          />
        )}

        <FormTableField
          name="data"
          labelOptions={{ hide: true }}
          showToolbar={false}
          showDetailBtnCond={stubFalse}
          showRadioCond={stubFalse}
          columns={memoizedColumns}
        />
      </Form>
    </>
  );
}

function useTableFieldColumn(column: ReportColumn): TableFieldColumn<any> {
  const SelectCellComponent = useSelectCellComponentFactory(column);

  let CellComponent: ComponentType<TableFieldCellProps<any>>;

  switch (column.type) {
    default:
    case ReportColumnType.TEXT:
      CellComponent = TableFieldCells.TextCell;
      break;
    case ReportColumnType.NUMBER:
      CellComponent = TableFieldCells.NumberCell;
      break;
    case ReportColumnType.BOOLEAN:
      CellComponent = TableFieldCells.BooleanCell;
      break;
    case ReportColumnType.DATE:
      CellComponent = TableFieldCells.DateCell;
      break;
    case ReportColumnType.DATETIME:
      CellComponent = TableFieldCells.DateTimeCell;
      break;
    case ReportColumnType.TIME:
      CellComponent = TableFieldCells.TimeCell;
      break;
    case ReportColumnType.SELECT:
      CellComponent = SelectCellComponent;
      break;
  }

  return {
    name: column.name,
    datakey: column.datakey,
    width: column.width,
    minWidth: column.minWidth,
    CellComponent,
  };
}

function useSelectCellComponentFactory(column: ReportColumn) {
  const source = useStaticListSource<DictionaryAutocomplete>(
    column.selectItems ?? []
  );
  return TableFieldCells.useSelectCellFactory<any, DictionaryAutocomplete>(
    () => source
  );
}
