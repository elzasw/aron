import { DictionaryObject, ScriptType, DatedObject } from 'common/common-types';

export interface Job extends DictionaryObject {
  timer: string;
  scriptType: ScriptType;
  script: string;
  running: boolean;
}

export enum RunState {
  STARTED = 'STARTED',
  ERROR = 'ERROR',
  FINISHED = 'FINISHED',
}

export interface Run extends DatedObject {
  job: Job;
  console: string;
  result: string;
  state: RunState;
  startTime: string;
  endTime: string;
}

export interface ScheduleProviderProps {
  jobUrl: string;
  runUrl: string;
}
