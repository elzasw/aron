import { useRef, useEffect, useContext } from 'react';
import { useDebouncedCallback } from 'use-debounce/lib';
import { useCrudSource } from 'utils/crud-source-hook';
import { useEventCallback } from 'utils/event-callback-hook';
import { EmptyComponent } from 'utils/empty-component';
import { useScrollableSource } from 'utils/scrollable-source-hook';
import { DomainObject } from 'common/common-types';
import { TableHandle } from 'composite/table/table-types';
import { DetailHandle } from 'composite/detail/detail-types';
import { EvidenceProps, EvidenceStateAction } from './evidence-types';
import { NavigationContext } from 'composite/navigation/navigation-context';

export function useEvidence<OBJECT extends DomainObject>({
  tableProps,
  detailProps,
  apiProps,
}: EvidenceProps<OBJECT>) {
  const tableSource = useScrollableSource<OBJECT>(apiProps.url + '/list');
  const crudSource = useCrudSource<OBJECT>(apiProps.url);

  const tableRef = useRef<TableHandle<OBJECT>>(null);
  const detailRef = useRef<DetailHandle<OBJECT>>(null);

  const { stateAction } = useContext(NavigationContext);
  useEffect(() => {
    if (stateAction?.action === EvidenceStateAction.NEW_ITEM) {
      // needs to do in next frame after Detail component is rendered
      requestAnimationFrame(() => {
        detailRef.current?.startNew(stateAction.data);
      });
    }

    if (stateAction?.action === EvidenceStateAction.SHOW_ITEM) {
      requestAnimationFrame(() => {
        tableRef.current?.setActiveRow(stateAction.data);
      });
    }
  }, [stateAction]);

  const handleActiveRowChange = useEventCallback((id: string | null) => {
    detailRef.current?.setActive(id);
  });

  const [handleActiveRowChangeDebounced] = useDebouncedCallback(
    handleActiveRowChange,
    500
  );

  const handleDetailPersisted = useEventCallback((id: string | null) => {
    tableRef.current?.refresh();
    tableRef.current?.setActiveRow(id);
  });

  const columns = tableProps?.columns ?? [];
  const FieldsComponent = detailProps?.FieldsComponent ?? EmptyComponent;

  return {
    columns,
    FieldsComponent,
    tableSource,
    crudSource,
    tableRef,
    detailRef,
    handleActiveRowChange: handleActiveRowChangeDebounced,
    handleDetailPersisted,
  };
}
