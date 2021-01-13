import { FileRef } from 'common/common-types';

export interface FileTableProps {
  value: FileRef[] | null | undefined;
  maxItems?: number;

  /**
   * Change handler.
   */
  onChange: (value: FileRef[]) => void;
  disabled?: boolean;
}
