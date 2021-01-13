import { DaoBundleType } from '../enums';
import { LinkedEntity } from './basic';

export interface Dao extends LinkedEntity {
  name: string;
  files: DaoFile[];
}

export interface DaoFile extends LinkedEntity {
  order: number;
  metadata: MetadataItem[]; // TODO: JSON?
  type: DaoBundleType;
  file: DaoFileFileType;
}

interface DaoFileFileType {
  id: string;
  name: string;
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
