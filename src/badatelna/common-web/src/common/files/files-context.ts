import { createContext } from 'react';
import { FileRef } from 'common/common-types';

export interface FilesContext {
  loading: boolean;
  url: string;
  uploadFile: (file: File) => Promise<FileRef | undefined>;
  getFileUrl: (id: string) => string;
}

export const FilesContext = createContext<FilesContext>(undefined as any);
