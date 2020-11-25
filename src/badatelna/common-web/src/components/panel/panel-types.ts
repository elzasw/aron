import { ReactNode } from 'react';

export interface PanelProps {
  label: ReactNode;
  expandable?: boolean;
  defaultExpanded?: boolean;
  sideBorder?: boolean;
}
