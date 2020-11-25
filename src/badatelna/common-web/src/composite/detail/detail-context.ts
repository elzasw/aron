import { DetailHandle } from './detail-types';
import { createContext } from 'react';
import { DomainObject } from 'common/common-types';

export type DetailContext<OBJECT extends DomainObject> = DetailHandle<OBJECT>;

export const DetailContext = createContext<DetailHandle<any>>(undefined as any);
