import { FileRef } from 'common/common-types';

export interface FileFieldProps {
  value: FileRef | null | undefined;

  /**
   * Change handler.
   */
  onChange: (value: FileRef | null) => void;
  disabled?: boolean;
}
