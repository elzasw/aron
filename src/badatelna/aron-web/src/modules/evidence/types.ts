import { ModulePath } from '../../enums';
import {
  ApuPartType,
  ApuPartItemType,
  Filter,
  Dao,
  ApuEntity,
  Facet,
  FilterConfig,
  ApuAttachment,
} from '../../types';
import {JsonType} from '../../enums';
import { TableData } from '../../components';

export interface FiltersChangeCallbackParams {
  query?: string;
  filters?: Filter[];
}

export type FiltersChangeCallback = (
  params: FiltersChangeCallbackParams
) => void;

export type FilterChangeCallBack = (f: FilterConfig) => void;

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

export interface DetailProps extends SectionProps {}

export interface MainProps extends SectionProps {}

export interface SidebarProps {
  apuPartItemTypes: ApuPartItemType[];
  facets: Facet[];
  path: ModulePath;
}

export interface ListProps {
  loading: boolean;
  items: any[];
  count: number;
}

export interface DetailDaoProps {
  apuInfo?: {
    name?: string,
    description?: string,
  };
  items: Dao[];
}

export interface DetailTreeProps {
  item: ApuEntity;
  id: string;
  verticalResize?: boolean;
}

export interface DetailAttachmentsProps {
  items: ApuAttachment[];
  setLoading: (loading: boolean) => void;
}

export interface Props extends EvidenceProps {}

export interface JsonDataBase {
    type: JsonType;
    data: any;
}

export interface JsonTable extends JsonDataBase {
    type: JsonType.TABLE;
    data: TableData;
}

export type JsonData = JsonTable;
