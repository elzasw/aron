import { createContext } from 'react';

export interface ScheduleRunsContext {
  jobsUrl: string;
}

export const ScheduleRunsContext = createContext<ScheduleRunsContext>({
  jobsUrl: '',
});
