import { createContext } from 'react';

export interface ScheduleContext {
  jobUrl: string;
  runUrl: string;
}

export const ScheduleContext = createContext<ScheduleContext>(undefined as any);
