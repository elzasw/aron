import { useRef } from 'react';
import { DialogHandle } from 'components/dialog/dialog-types';
import { useEventCallback } from 'utils/event-callback-hook';
import {
  Report,
  ReportingExportConfiguration,
  ReportColumn,
  ReportColumnType,
} from '../reporting-types';
import { ReportDefinition } from 'index';
import { TableCells } from 'composite/table/table-cells';

export function useReportExports({
  report,
  definition,
}: {
  report: Report | null;
  definition: ReportDefinition | undefined;
}) {
  const exportDialogRef = useRef<DialogHandle>(null);
  const openExportDialog = useEventCallback(() =>
    exportDialogRef.current?.open()
  );
  const closeExportDialog = useEventCallback(() =>
    exportDialogRef.current?.close()
  );

  const provideData = useEventCallback(() => {
    if (definition != null && report != null) {
      return {
        title: definition.label,
        definitionId: definition.id,
        columns: serializeReportColumns(report.columns),
      } as ReportingExportConfiguration;
    } else {
      return {};
    }
  });

  return {
    exportDialogRef,
    openExportDialog,
    closeExportDialog,
    provideData,
  };
}

function serializeReportColumns(columns: ReportColumn[]) {
  return columns.map((column) => {
    const CellComponent = getCellComponent(column);
    const valueMapperName =
      column.type === ReportColumnType.SELECT
        ? 'selectColumnMapper'
        : undefined;
    const valueMapperData =
      column.type === ReportColumnType.SELECT ? column.selectItems : undefined;

    return {
      name: column.name,
      datakey: column.datakey,
      displaykey: column.datakey,
      width: column.width,
      visible: true,
      cellComponentName: CellComponent.displayName ?? CellComponent.name,
      valueMapperName,
      valueMapperData,
    };
  });
}

function getCellComponent(column: ReportColumn) {
  switch (column.type) {
    case ReportColumnType.TEXT:
      return TableCells.TextCell;
    case ReportColumnType.NUMBER:
      return TableCells.NumberCell;
    case ReportColumnType.BOOLEAN:
      return TableCells.BooleanCell;
    case ReportColumnType.DATE:
      return TableCells.DateCell;
    case ReportColumnType.DATETIME:
      return TableCells.DateTimeCell;
    case ReportColumnType.TIME:
      return TableCells.TimeCell;
    case ReportColumnType.SELECT:
      return TableCells.TextCell;
  }
}
