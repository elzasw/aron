import { FileRef } from 'common/common-types';

export interface FileTableProps {
  value: FileRef[] | null | undefined;

  /**
   * Change handler.
   */
  onChange: (value: FileRef[]) => void;
  disabled?: boolean;
}
