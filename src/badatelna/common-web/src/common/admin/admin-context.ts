import { createContext } from 'react';

export interface AdminContext {
  reindexUrl: string;
}

export const AdminContext = createContext<AdminContext>({ reindexUrl: '' });
