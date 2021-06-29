import { ComponentType, ReactNode } from 'react';
import { CustomSettings } from 'common/settings/user/user-settings-types';
import { Report, ReportDefinition } from 'modules/reporting/reporting-types';

export interface CardAddProps {}

export interface CardActionProps {
  navigate?: () => void;
  title: string;
  value?: string;
}

export interface CardProps {
  report?: Report;
  load: () => Promise<void>;
  loading: boolean;
  definitionId: string;
}

export interface CardCustomProps {
  navigate: () => void;
  children?: ReactNode;
  actions?: ReactNode;
  definitionId: string;
}

export interface DashboardCardProps {
  id: string;
}

export interface DashboardUserSettings extends CustomSettings {
  items?: string[];
}

export interface CardSettingsDialogProps {
  report: Report;
  definition: ReportDefinition;
  load: () => Promise<void>;
}

export interface DashboardProps {
  cardFactory?: (definitionId: string) => ComponentType<CardProps> | undefined;
  classes?: DashboardClasses;
}

export interface DashboardClasses {
  card?: string;
  cardAction?: string;
  cardActionValue?: string;
  cardUniversalTitle?: string;
  cardUniversalValue?: string;
}
