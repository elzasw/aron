import { createContext, ComponentType } from 'react';
import { noop } from 'lodash';
import { ReportDefinition } from 'modules/reporting/reporting-types';
import { CardProps, DashboardClasses } from './dashboard-types';

export interface DashboardContext {
  definitions: ReportDefinition[];
  classes?: DashboardClasses;
  cardFactory: (definitionId: string) => ComponentType<CardProps> | undefined;
  loadDashboard: () => string[];
  saveDashboard: (items: string[]) => void;
  openAddDialog: () => void;
  remove: (id: string) => void;
}

export const DashboardContext = createContext<DashboardContext>({
  definitions: [],
  cardFactory: () => undefined,
  loadDashboard: () => [],
  saveDashboard: noop,
  openAddDialog: noop,
  remove: noop,
});
