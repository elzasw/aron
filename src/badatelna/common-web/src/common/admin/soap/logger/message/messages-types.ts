import { AuthoredObject } from 'common/common-types';

export interface SoapMessage extends AuthoredObject {
  service: string;
  request: string;
  response: string;
}
