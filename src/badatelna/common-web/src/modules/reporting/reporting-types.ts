import { AuthoredObject, Params } from 'common/common-types';

export interface Report extends AuthoredObject {
  definitionId: string;
  configuration: any;
  data: any;
  columns: ReportColumn[];
}

export enum ReportColumnType {
  TEXT = 'TEXT',
  NUMBER = 'NUMBER',
  BOOLEAN = 'BOOLEAN',
  DATE = 'DATE',
  DATETIME = 'DATETIME',
  TIME = 'TIME',
  SELECT = 'SELECT',
}

export interface ReportColumn {
  name: string;
  datakey: string;
  width: number;
  minWidth: number;
  type: ReportColumnType;

  selectItems?: SelectItem[];
}

export interface ReportDefinition {
  id: string;
  groupId: string;
  label: string;
  groupLabel: string;
  inputFields: ReportInputField[];
  autogenerate: boolean;
  dashboardSupport?: boolean;
}

export interface ReportInputField {
  name: string;
  label: string;
  type: ReportInputFieldType;
  selectItems?: SelectItem[];
  autocompleteUrl?: string;
  autocompleteApiUrl?: string;
  autocompleteParams?: Params;
}

export interface SelectItem {
  id: string;
  name: string;
}

export enum ReportInputFieldType {
  TEXT = 'TEXT',
  NUMBER = 'NUMBER',
  BOOLEAN = 'BOOLEAN',
  DATE = 'DATE',
  DATETIME = 'DATETIME',
  TIME = 'TIME',
  SELECT = 'SELECT',
  AUTOCOMPLETE = 'AUTOCOMPLETE',
}

export interface ReportingExportConfiguration {
  definitionId: string;
  columns: any[];
  title: string;
}

export interface ReportingProviderProps {
  url: string;
}

export interface ReportSettingsFormProps {
  definition?: ReportDefinition;
}
