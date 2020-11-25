import { ApuPartItemDataType, ApuType } from '../enums';
import { Entity, LinkedEntity } from './basic';

export interface ApuEntity extends LinkedEntity {
  name: string;
  description: string;
  published: boolean;
  source: ApuSource;
  parent?: ApuEntity;
  parts?: ApuPart[];
  attachments?: ApuAttachment[];
  type: ApuType;
}

export interface ApuPart extends Entity {
  value: string;
  type: string;
  items: ApuPartItem[];
}

export interface ApuPartItem extends Entity {
  value: string;
  visible: boolean;
  href: string;
  type: string;
}

export interface ApuAttachment extends Entity {
  name: string;
  file: any;
}

export interface ApuSource extends Entity {
  published: string;
  data: string;
}

export interface ApuPartType extends Entity {
  code: string;
  label: string;
  order: number;
}

export interface ApuPartItemType extends Entity {
  code: string;
  label: string;
  order: number;
  dataType: ApuPartItemDataType;
}
