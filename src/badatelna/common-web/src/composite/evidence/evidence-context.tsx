import { createContext, RefObject } from 'react';
import { TableHandle } from 'composite/table/table-types';
import { DetailHandle } from 'composite/detail/detail-types';
import { ScrollableSource, CrudSource } from 'common/common-types';

export const EvidenceContext = createContext<{
  apiUrl: string;
  tableSource: ScrollableSource<any>;
  crudSource: CrudSource<any>;
  tableRef: RefObject<TableHandle<any>>;
  detailRef: RefObject<DetailHandle<any>>;
}>(undefined as any);
