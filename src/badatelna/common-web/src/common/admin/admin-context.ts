import { createContext } from 'react';

export interface AdminContext {
  reindexUrl: string;
  soapMessagesUrl: string;
}

export const AdminContext = createContext<AdminContext>({
  reindexUrl: '',
  soapMessagesUrl: '',
});
