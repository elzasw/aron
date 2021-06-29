import { ScriptType } from 'common/common-types';

export interface ExecuteScriptRequest {
  script: string;
  scriptType: ScriptType;
  useTransaction: boolean;
  params?: Record<string, any>;
}

export interface ExecuteScriptResponse {
  result: any;
  error?: string;
  duration: number;
}

export interface AdminConsoleInput {
  script: string;
  scriptType: ScriptType;
  useTransaction: boolean;
}
