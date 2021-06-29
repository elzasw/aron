import {
  DictionaryObject,
  FileRef,
  AuthoredObject,
  ListSource,
  DictionaryAutocomplete,
} from 'common/common-types';

export interface ExportTemplate extends DictionaryObject {
  content?: FileRef;
  dataProvider?: ExportDataProvider;
  designProvider?: ExportDesignProvider;
  configuration?: string;
  tags?: string[];
  label?: string;
  allowedTypes?: ExportType[];
  allowedPermissions?: string[];
  restrictByPermission?: boolean;
}

export enum ExportType {
  DOCX = 'DOCX',
  XLSX = 'XLSX',
  PDF = 'PDF',
  HTML = 'HTML',
  XML = 'XML',
  CSV = 'CSV',
}

export enum ExportDataProvider {
  CONFIGURATION_PROVIDER = 'CONFIGURATION_PROVIDER',
  NOOP_PROVIDER = 'NOOP_PROVIDER',
  PARAMS_PROVIDER = 'PARAMS_PROVIDER',
  REPORTING_PROVIDER = 'REPORTING_PROVIDER',
  SINGLE_ENTITY_PROVIDER = 'SINGLE_ENTITY_PROVIDER',
}

export enum ExportDesignProvider {
  DYNAMIC_PROVIDER = 'DYNAMIC_PROVIDER',
  SIMPLE_PROVIDER = 'SIMPLE_PROVIDER',
}

export enum ExportRequestState {
  /**
   * State of a export request after creation
   */
  PENDING = 'PENDING',

  /**
   * State of a export request being processed
   */
  PROCESSING = 'PROCESSING',

  /**
   * State of a request successfully processed and corresponding export file was generated
   */
  PROCESSED = 'PROCESSED',

  /**
   * State of a export request whose processing failed
   */
  FAILED = 'FAILED',
}

export interface ExportRequest extends AuthoredObject {
  template?: ExportTemplate;
  configuration?: string;
  type?: ExportType;
  priority?: number;

  state?: ExportRequestState;
  result?: FileRef;

  /**
   * Message (e.g. error that occurred when processing this export request)
   */
  message?: string;

  processingStart?: string;
  processingEnd?: string;
}

export interface ExportProviderProps {
  url: string;
  tags: ListSource<DictionaryAutocomplete>;
  disableSync?: boolean;
  disableAsync?: boolean;
}
