import { FileRef } from 'common/common-types';

export interface FileFieldProps {
  value: FileRef | null | undefined;

  /**
   * Change handler.
   */
  onChange: (value: FileRef | null) => void;
  disabled?: boolean;
  /**
   * Array of accepted formats
   * if empty/undefined, all formats will be accepted
   * @see https://developer.mozilla.org/en-US/docs/Web/HTML/Attributes/accept
   */
  accept?: string[];

  /**
   * Show upload icon.
   *
   * default: true
   */
  showUpload?: boolean;

  /**
   * Show clear icon.
   *
   * default: true
   */
  showClear?: boolean;

  /**
   * Custom download url.
   *
   * used when desired download url differs from one declared in FilesProvider component.
   */
  customDownloadUrl?: string;
}
