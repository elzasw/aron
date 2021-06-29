import { ReactNode } from 'react';

export interface PanelProps {
  className?: string;
  label: ReactNode;
  summary?: ReactNode;
  expandable?: boolean;
  defaultExpanded?: boolean;
  sideBorder?: boolean;
  fitHeight?: boolean;
}

export interface PanelHandle {
  setExpanded: (expanded: boolean) => void;
  scrollIntoView: () => void;
}
