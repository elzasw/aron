import { DictionaryObject, UserReference } from 'common/common-types';

export enum EventSeverity {
  DEBUG = 'DEBUG',
  INFO = 'INFO',
  WARN = 'WARN',
  ERROR = 'ERROR',
}

export interface AlogEvent extends DictionaryObject {
  /**
   * Source of event.
   */
  source: string;

  /**
   * IpAddress of the caller.
   */
  ipAddress: string;

  /**
   * Severity of the event.
   */
  severity: EventSeverity;

  /**
   * Message of the event.
   */
  message: string;

  /**
   * JSON formated detail of the event.
   */
  detail: string;

  user: UserReference;
}
