import { LinkedEntity } from './basic';

export interface Dao extends LinkedEntity {
  description: string;
  files: DaoFile[];
}

export interface DaoFile extends LinkedEntity {
  position: number;
  metadata: MetadataItem[]; // TODO: JSON?
}

interface MetadataItem {
  value: string;
  type: MetadataType;
}

interface MetadataType {
  name: string;
  code: string;
  order: number;
}
