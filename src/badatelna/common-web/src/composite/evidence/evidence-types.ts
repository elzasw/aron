import { TableProps } from 'composite/table/table-types';
import { DetailProps } from 'composite/detail/detail-types';
import { DomainObject, Params } from 'common/common-types';
import { AbortableFetch } from 'utils/abortable-fetch';

export type EvidenceTableProps<OBJECT> = Omit<
  TableProps<OBJECT>,
  'disabled' | 'source' | 'columns' | 'tableId' | 'version'
> &
  Partial<Pick<TableProps<OBJECT>, 'columns'>>;

export type EvidenceSwitcherProps = {
  leftLabel?: string;
  rightLabel?: string;
  hideMenuTools?: boolean;
};

export type EvidenceDetailProps<OBJECT extends DomainObject> = Omit<
  DetailProps<OBJECT>,
  'source' | 'width'
>;

export enum EvidenceScreenMode {
  TABLE = 0,
  SPLIT,
  DETAIL,
}

export enum EvidenceStateAction {
  NEW_ITEM = 'NEW_ITEM',
  SHOW_ITEM = 'SHOW_ITEM',
}

export interface EvidenceApiProps<OBJECT extends DomainObject> {
  /**
   * Base url of API evidence.
   */
  url: string;

  /**
   * Custom get item function.
   */
  getItem?: (api: string, itemId: string) => AbortableFetch;

  /**
   * Custom create item function.
   */
  createItem?: (api: string, item: OBJECT) => AbortableFetch;

  /**
   * Custom update item function.
   */
  updateItem?: (
    api: string,
    item: OBJECT,
    _initialItem: OBJECT
  ) => AbortableFetch;

  /**
   * Custom delete item function.
   */
  deleteItem?: (api: string, itemId: string) => AbortableFetch;

  /**
   * Custom list items function.
   */
  listItems?: (api: string, params: Params) => AbortableFetch;
}

export interface EvidenceProps<OBJECT extends DomainObject> {
  /**
   * Unique identifier.
   */
  identifier: string;

  /**
   * Evidence version.
   *
   * Used for settings versioning.
   * Default to 1.
   */
  version?: number;

  apiProps: EvidenceApiProps<OBJECT>;

  switcherProps?: EvidenceSwitcherProps;

  tableProps?: EvidenceTableProps<OBJECT>;

  detailProps?: EvidenceDetailProps<OBJECT>;
}
