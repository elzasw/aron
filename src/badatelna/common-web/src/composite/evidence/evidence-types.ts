import { TableProps } from 'composite/table/table-types';
import { ComponentType } from 'react';
import { DetailProps } from 'composite/detail/detail-types';
import { DomainObject } from 'common/common-types';

export type EvidenceTableProps<OBJECT> = Omit<
  TableProps<OBJECT>,
  'disabled' | 'source' | 'columns'
> &
  Partial<Pick<TableProps<OBJECT>, 'columns'>>;

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

export interface EvidenceSwitcherProps {
  screenMode: EvidenceScreenMode;
  onChange: (screenMode: EvidenceScreenMode) => void;
}

export interface EvidenceApiProps {
  /**
   * Base url of API evidence.
   */
  url: string;
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

  apiProps: EvidenceApiProps;

  tableProps?: EvidenceTableProps<OBJECT>;

  detailProps?: EvidenceDetailProps<OBJECT>;

  SwitcherComponent?: ComponentType<EvidenceSwitcherProps>;
}
