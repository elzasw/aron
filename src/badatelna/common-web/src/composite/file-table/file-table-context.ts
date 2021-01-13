import { createContext } from 'react';

export interface FileTableContext {
  maxItems?: number;
}

export const FileTableContext = createContext<FileTableContext>({});
