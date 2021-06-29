import { AuthoredObject } from 'common/common-types';

export interface HistoryOperationReference {
  id: string;
  name: string;
}

export interface History extends AuthoredObject {
  entityId: string;
  operation: HistoryOperationReference;
  description: string;
}

export interface HistoryProps {
  id: string;
}
