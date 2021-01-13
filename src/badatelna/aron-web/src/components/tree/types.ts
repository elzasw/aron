export interface Props {
  items: any[];
  className?: string;
  disableClick?: any;
  expanded?: string[];
  selected?: string[];
  labelMapper?: (item: any) => string;
  idMapper?: (item: any) => string;
  onLabelClick?: (item: any) => void;
  onNodeToggle?: (expanded: string[]) => void;
}
