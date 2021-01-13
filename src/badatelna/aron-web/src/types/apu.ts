import { ApuPartItemDataType, ApuType, ApuPartViewType } from '../enums';
import { Entity, LinkedEntity } from './basic';
import { Dao } from './dao';

export interface ApuEntity extends LinkedEntity {
  name: string;
  description: string;
  published: boolean;
  source: ApuSource;
  type: ApuType;
  parent?: ApuEntity;
  parts?: ApuPart[];
  attachments?: ApuAttachment[];
  digitalObjects?: Dao[];
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
  name: string;
  viewType: ApuPartViewType;
}

export interface ApuPartItemType extends Entity {
  code: string;
  name: string;
  type: ApuPartItemDataType;
}

export interface ApuTree extends Entity {
  name: string;
  type: ApuType;
  children: ApuTree[];
}
