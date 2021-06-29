import {
  AuthoredObject,
  DomainObject,
  FileRef,
  UserReference,
} from 'common/common-types';

export interface SignContent extends DomainObject {
  toSign?: FileRef;
  signed?: FileRef;
}

export enum SignRequestState {
  NEW = 'NEW',
  SIGNED = 'SIGNED',
  CANCELED = 'CANCELED',
  ERROR = 'ERROR',
}

export interface SignRequest extends AuthoredObject {
  contents: SignContent[];
  user: UserReference;
  state: SignRequestState;
  error?: string;
}

export interface UploadSignedContentDto {
  content: SignContent | null;
  signed: FileRef | null;
}

export interface SigningProviderProps {
  url: string;
  reportTag: string | null;
}
