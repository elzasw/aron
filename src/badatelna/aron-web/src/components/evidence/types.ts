import { ModulePath } from '../../enums';
import { ApuPartType, ApuPartItemType, Filter, Dao } from '../../types';

export type FilterObject = { [key: string]: any };

export type FilterData = { name: string; filterObject: FilterObject };

export interface FiltersChangeCallbackParams {
  query?: string;
  filters?: Filter[];
}

export type FiltersChangeCallback = (
  params: FiltersChangeCallbackParams
) => void;

export type FilterChangeCallBack = (
  name: string,
  f: FilterObject | null
) => void;

interface PropsBase {
  label: string;
  apuPartTypes: ApuPartType[];
  apuPartItemTypes: ApuPartItemType[];
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
  path: ModulePath;
  query?: string;
  filters: any[];
  onChange: FiltersChangeCallback;
}

export interface ListProps {
  items: any[];
}

export interface DetailDaoProps {
  item: Dao;
}

export interface DetailDaoDialogProps {
  open: boolean;
  item: Dao;
  onClose: () => void;
}

export interface Props extends EvidenceProps {}
