import * as React from 'react';
import { FileProviderProps } from './files-types';
import { FilesContext } from './files-context';
import { useFiles } from './files-hook';

export function FilesProvider({
  children,
  url,
}: React.PropsWithChildren<FileProviderProps>) {
  const { context } = useFiles(url);
  return (
    <FilesContext.Provider value={context}>{children}</FilesContext.Provider>
  );
}
