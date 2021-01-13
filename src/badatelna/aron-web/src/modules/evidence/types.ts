import { ModulePath, FilterType } from '../../enums';
import {
  ApuPartType,
  ApuPartItemType,
  Filter,
  Dao,
  ApiFilterOperation,
  ApuEntity,
  Facet,
  Relationship,
} from '../../types';

export type FilterObject = { [key: string]: any };

export type FilterData = {
  field: string;
  filterObject: FilterObject;
  operation?: ApiFilterOperation;
  filters?: Filter[];
  type?: FilterType;
};

export interface FiltersChangeCallbackParams {
  query?: string;
  filters?: Filter[];
}

export type FiltersChangeCallback = (
  params: FiltersChangeCallbackParams
) => void;

export type FilterChangeCallBack = (
  name: string,
  f: FilterObject | null,
  operation?: ApiFilterOperation,
  filters?: Filter[],
  type?: FilterType
) => void;

interface PropsBase {
  label: string;
  apuPartTypes: ApuPartType[];
  apuPartItemTypes: ApuPartItemType[];
  facets: Facet[];
}

export interface EvidenceProps extends PropsBase {
  path: ModulePath;
}

export interface SectionProps extends PropsBase {
  modulePath: ModulePath;
}

export interface DetailProps extends SectionProps { }

export interface MainProps extends SectionProps { }

export interface SidebarProps {
  path: ModulePath;
  query?: string;
  filters: any[];
  onChange: FiltersChangeCallback;
  apuPartItemTypes: ApuPartItemType[];
  relationships: Relationship[] | null;
  setRelationships: (r: Relationship[] | null) => void;
}

export interface ListProps {
  loading: boolean;
  items: any[];
  count: number;
  page: number;
  pageSize: number;
  updatePage: (n: number) => void;
  updatePageSize: (n: number) => void;
}

export interface DetailDaoProps {
  items: Dao[];
}

export interface DetailDaoDialogProps {
  item: Dao;
  items: Dao[];
  setItem: (item: Dao | null) => void;
}

export interface DetailTreeProps {
  item: ApuEntity;
  id: string;
}

export interface Props extends EvidenceProps { }
