import { DatedObject, UserReference } from 'common/common-types';

export enum PersonalEventType {
  LOGIN_SUCCESSFUL = 'LOGIN_SUCCESSFUL',
  LOGIN_FAILED = 'LOGIN_FAILED',
  LOGOUT = 'LOGOUT',
  LOGOUT_AUTOMATIC = 'LOGOUT_AUTOMATIC',
}

export interface PersonalEvent extends DatedObject {
  type: PersonalEventType;
  user: UserReference;
  data?: string;
}
