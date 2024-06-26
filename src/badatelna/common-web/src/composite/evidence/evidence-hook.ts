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
import { useEvidenceItemRedirect } from './hook/item-redirect-hook';
import { SplitScreenHandle } from 'components/split-screen/split-screen-types';

export function useEvidence<OBJECT extends DomainObject>({
  tableProps,
  detailProps,
  apiProps,
}: EvidenceProps<OBJECT>) {
  useEvidenceItemRedirect();

  const tableSource = useScrollableSource<OBJECT>({
    url: apiProps.url + '/list',
    listItems: apiProps.listItems,
  });
  const crudSource = useCrudSource<OBJECT>({
    url: apiProps.url,
    getItem: apiProps.getItem,
    createItem: apiProps.createItem,
    updateItem: apiProps.updateItem,
    deleteItem: apiProps.deleteItem,
  });

  const tableRef = useRef<TableHandle<OBJECT>>(null);
  const detailRef = useRef<DetailHandle<OBJECT>>(null);
  const splitScreenRef = useRef<SplitScreenHandle>(null);

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
    detailRef.current?.formRef?.resetValidation();

    const isFullscreenTable = splitScreenRef.current?.isLeftOnFullscreen();
    if (isFullscreenTable) {
      splitScreenRef.current?.handleMoveToMiddle();
    }
    tableProps?.onActiveChange?.(id);
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
    splitScreenRef,
    detailRef,
    handleActiveRowChange: handleActiveRowChangeDebounced,
    handleDetailPersisted,
  };
}
