import { ModulePath } from "../../enums";

export interface SidebarProps {}

export interface ContentProps {
  onClick: (id: string) => void;
  items: any[];
}

export interface EvidenceProps {
  path: ModulePath;
  label: string;
}

export interface Props extends EvidenceProps {}
